"""Single deployable entry point for the Python backend.

Wires the FastAPI HTTP surface (used for the k6 /api/perf shootout probe)
alongside a grpc.aio server exposing OrdersService / PaymentsService /
NotificationsService as defined in proto/ecommerce.proto. Only each
module's public-api is imported here — internal/ and infrastructure/
packages are never reached from outside their own module.
"""
import asyncio
import time

import uvicorn
from fastapi import FastAPI, Request

from modules.orders.public_api.service import OrdersPublicApi
from modules.payments.public_api.service import PaymentsPublicApi
from modules.notifications.public_api.service import NotificationsPublicApi
from modules.catalog.public_api.service import CatalogPublicApi
from modules.payments.internal.webhook_handler import verify_and_parse_webhook, StripeSignatureError

app = FastAPI(title="ecommerce-backend-python")

orders_api = OrdersPublicApi()
payments_api = PaymentsPublicApi()
notifications_api = NotificationsPublicApi()
catalog_api = CatalogPublicApi()


@app.get("/api/products")
async def list_products(category: str | None = None):
    return catalog_api.list_products(category)


@app.get("/api/products/{sku}")
async def get_product(sku: str):
    product = catalog_api.get_product(sku)
    if product is None:
        return {"error": "not found"}
    return product


@app.get("/api/products/search")
async def search_products(q: str):
    return catalog_api.search_products(q)


@app.post("/webhooks/stripe")
async def stripe_webhook(request: Request):
    """Real Stripe webhook endpoint: verifies the signature header before
    trusting any payload, then dispatches on event type to drive the
    orders/payments state machine. See
    modules/payments/internal/webhook_handler.py for signature verification
    and docs/stripe-webhooks.md for the cross-language pattern this follows.
    """
    payload = await request.body()
    sig_header = request.headers.get("stripe-signature", "")
    try:
        event = verify_and_parse_webhook(payload, sig_header)
    except StripeSignatureError as e:
        return {"error": str(e)}, 400

    event_type = event.get("type", "")
    data_obj = event.get("data", {}).get("object", {})
    order_id = data_obj.get("metadata", {}).get("order_id")
    payment_intent_id = data_obj.get("id", "")

    if event_type == "payment_intent.succeeded" and order_id:
        capture_result = payments_api.capture(payment_intent_id)
        orders_api.mark_paid(order_id, payment_intent_id)
        notifications_api.send_order_confirmation(order_id, data_obj.get("customer", ""), "EMAIL")
    elif event_type == "payment_intent.payment_failed" and order_id:
        orders_api.mark_failed(order_id, data_obj.get("last_payment_error", {}).get("message", "payment failed"))
    elif event_type == "charge.refunded" and order_id:
        orders_api.mark_refunded(order_id, data_obj.get("amount_refunded", 0))

    return {"received": True, "event_type": event_type}


@app.get("/api/perf")
async def perf():
    """Benchmark probe hit by shared-benchmarks/load-test.js."""
    return {
        "backend": "backend-python",
        "language": "python",
        "server_time_unix_ms": int(time.time() * 1000),
        "status": "ok",
    }


@app.get("/healthz")
async def healthz():
    return {"status": "ok"}


@app.post("/api/orders")
async def create_order(payload: dict):
    return orders_api.create_order(payload)


if __name__ == "__main__":
    import threading
    from grpc_server import serve as serve_grpc

    grpc_server = serve_grpc(port=50051)
    threading.Thread(target=grpc_server.wait_for_termination, daemon=True).start()

    uvicorn.run(app, host="0.0.0.0", port=8080)
