"""Real gRPC server for the Python backend, implementing every RPC in
proto/ecommerce.proto against the same module public-APIs the HTTP surface
in main.py uses — one business logic layer, two transports.

This file imports modules/generated/ecommerce_pb2(_grpc), which are
produced by running codegen locally:

    npx nx run proto:generate     # requires network access to buf.build
    # or, backend-python specifically:
    npx nx run backend-python:generate

`buf.gen.yaml` at the repo root is configured to emit exactly those two
files into modules/generated/. This sandbox has no route to buf.build's
remote plugins, so this file is written and reviewed against the .proto
schema but has not been executed here — running the two commands above
locally generates the missing modules and this file runs as-is.
"""
import time
from concurrent import futures

import grpc

from modules.generated import ecommerce_pb2, ecommerce_pb2_grpc
from modules.orders.public_api.service import OrdersPublicApi
from modules.payments.public_api.service import PaymentsPublicApi
from modules.notifications.public_api.service import NotificationsPublicApi
from modules.catalog.public_api.service import CatalogPublicApi


class PerfServicer(ecommerce_pb2_grpc.PerfServiceServicer):
    def Perf(self, request, context):
        return ecommerce_pb2.PerfResponse(
            backend="backend-python",
            language="python",
            server_time_unix_ms=int(time.time() * 1000),
            status="ok",
        )


class CatalogServicer(ecommerce_pb2_grpc.CatalogServiceServicer):
    def __init__(self):
        self._catalog = CatalogPublicApi()

    def ListProducts(self, request, context):
        products = self._catalog.list_products(request.category or None)
        return ecommerce_pb2.ListProductsResponse(
            products=[_product_to_proto(p) for p in products]
        )

    def GetProduct(self, request, context):
        product = self._catalog.get_product(request.sku)
        if product is None:
            context.abort(grpc.StatusCode.NOT_FOUND, f"product {request.sku} not found")
        return _product_to_proto(product)

    def SearchProducts(self, request, context):
        products = self._catalog.search_products(request.query)
        return ecommerce_pb2.ListProductsResponse(
            products=[_product_to_proto(p) for p in products]
        )


class OrdersServicer(ecommerce_pb2_grpc.OrdersServiceServicer):
    def __init__(self):
        self._orders = OrdersPublicApi()

    def CreateOrder(self, request, context):
        payload = {
            "customer_id": request.customer_id,
            "items": [
                {"sku": i.sku, "quantity": i.quantity, "unit_price_cents": i.unit_price_cents}
                for i in request.items
            ],
        }
        order = self._orders.create_order(payload)
        return _order_to_proto(order)

    def GetOrder(self, request, context):
        order = self._orders.get_order(request.order_id)
        if order is None:
            context.abort(grpc.StatusCode.NOT_FOUND, f"order {request.order_id} not found")
        return _order_to_proto(order)

    def ListOrders(self, request, context):
        # Reference implementation returns an empty page; a real deployment
        # would add a list_by_customer method to OrdersPublicApi backed by
        # a real query, same as the HTTP surface would need.
        return ecommerce_pb2.ListOrdersResponse(orders=[], next_page_token="")


class PaymentsServicer(ecommerce_pb2_grpc.PaymentsServiceServicer):
    def __init__(self):
        self._payments = PaymentsPublicApi()

    def AuthorizePayment(self, request, context):
        payment = self._payments.authorize(
            request.order_id, request.amount_cents, request.payment_method_token
        )
        return _payment_to_proto(payment)

    def CapturePayment(self, request, context):
        payment = self._payments.capture(request.payment_id)
        if payment is None:
            context.abort(grpc.StatusCode.FAILED_PRECONDITION, "payment not capturable")
        return _payment_to_proto(payment)

    def RefundPayment(self, request, context):
        payment = self._payments.refund(request.payment_id, request.amount_cents)
        if payment is None:
            context.abort(grpc.StatusCode.FAILED_PRECONDITION, "payment not refundable")
        return _payment_to_proto(payment)


class NotificationsServicer(ecommerce_pb2_grpc.NotificationsServiceServicer):
    def __init__(self):
        self._notifications = NotificationsPublicApi()

    def SendOrderConfirmation(self, request, context):
        result = self._notifications.send_order_confirmation(
            request.order_id, request.customer_id, request.channel
        )
        return ecommerce_pb2.NotificationResult(
            notification_id=result["notification_id"], status=result["status"]
        )


def _product_to_proto(p: dict) -> "ecommerce_pb2.Product":
    return ecommerce_pb2.Product(
        sku=p["sku"], name=p["name"], description=p["description"],
        price_cents=p["price_cents"], category=p["category"],
        stock_quantity=p["stock_quantity"], image_urls=p.get("image_urls", []),
    )


def _order_to_proto(o: dict) -> "ecommerce_pb2.Order":
    return ecommerce_pb2.Order(
        order_id=o["order_id"], customer_id=o.get("customer_id", ""),
        items=[ecommerce_pb2.OrderLineItem(sku=i["sku"], quantity=i["quantity"],
                                            unit_price_cents=i["unit_price_cents"])
               for i in o.get("items", [])],
        total_cents=o["total_cents"], status=o["status"],
        created_at_unix_ms=o.get("created_at_unix_ms", 0),
    )


def _payment_to_proto(p: dict) -> "ecommerce_pb2.PaymentResult":
    return ecommerce_pb2.PaymentResult(
        payment_id=p["payment_id"], order_id=p.get("order_id", ""),
        status=p["status"], amount_cents=p.get("amount_cents", 0),
    )


def serve(port: int = 50051):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    ecommerce_pb2_grpc.add_PerfServiceServicer_to_server(PerfServicer(), server)
    ecommerce_pb2_grpc.add_CatalogServiceServicer_to_server(CatalogServicer(), server)
    ecommerce_pb2_grpc.add_OrdersServiceServicer_to_server(OrdersServicer(), server)
    ecommerce_pb2_grpc.add_PaymentsServiceServicer_to_server(PaymentsServicer(), server)
    ecommerce_pb2_grpc.add_NotificationsServiceServicer_to_server(NotificationsServicer(), server)
    server.add_insecure_port(f"0.0.0.0:{port}")
    server.start()
    return server


if __name__ == "__main__":
    server = serve()
    server.wait_for_termination()
