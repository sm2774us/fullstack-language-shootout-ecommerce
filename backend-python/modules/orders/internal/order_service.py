"""Business logic — hidden from the outside world. Not importable cross-module."""
import time
import uuid
from modules.orders.infrastructure.repository import OrderRepository

class OrderService:
    def __init__(self):
        self._repo = OrderRepository()

    def create_order(self, payload: dict) -> dict:
        items = payload.get("items", [])
        total = sum(i["quantity"] * i["unit_price_cents"] for i in items)
        order = {
            "order_id": str(uuid.uuid4()),
            "customer_id": payload.get("customer_id"),
            "items": items,
            "total_cents": total,
            "status": "PENDING",
            "created_at_unix_ms": int(time.time() * 1000),
        }
        self._repo.save(order)
        return order

    def get_order(self, order_id: str) -> dict:
        return self._repo.find(order_id)

    def mark_paid(self, order_id: str, payment_id: str) -> dict | None:
        """Transitions PENDING -> PAID. Called by the payments module's
        webhook handler once a Stripe payment_intent.succeeded event has
        been signature-verified — never on unverified input."""
        order = self._repo.find(order_id)
        if order is None:
            return None
        order["status"] = "PAID"
        order["payment_id"] = payment_id
        self._repo.save(order)
        return order

    def mark_failed(self, order_id: str, reason: str) -> dict | None:
        """Transitions PENDING -> CANCELLED. Called on a verified
        payment_intent.payment_failed webhook event."""
        order = self._repo.find(order_id)
        if order is None:
            return None
        order["status"] = "CANCELLED"
        order["cancel_reason"] = reason
        self._repo.save(order)
        return order

    def mark_refunded(self, order_id: str, amount_cents: int) -> dict | None:
        """Transitions PAID -> REFUNDED. Called by PaymentsService.RefundPayment
        after the payments module confirms the refund with Stripe."""
        order = self._repo.find(order_id)
        if order is None or order.get("status") != "PAID":
            return None
        order["status"] = "REFUNDED"
        order["refunded_cents"] = amount_cents
        self._repo.save(order)
        return order
