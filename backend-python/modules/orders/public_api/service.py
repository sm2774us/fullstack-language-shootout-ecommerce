"""ONLY interface other modules/apps may import from the orders module."""
from modules.orders.internal.order_service import OrderService

class OrdersPublicApi:
    def __init__(self):
        self._svc = OrderService()

    def create_order(self, payload: dict) -> dict:
        return self._svc.create_order(payload)

    def get_order(self, order_id: str) -> dict:
        return self._svc.get_order(order_id)

    def mark_paid(self, order_id: str, payment_id: str) -> dict | None:
        return self._svc.mark_paid(order_id, payment_id)

    def mark_failed(self, order_id: str, reason: str) -> dict | None:
        return self._svc.mark_failed(order_id, reason)

    def mark_refunded(self, order_id: str, amount_cents: int) -> dict | None:
        return self._svc.mark_refunded(order_id, amount_cents)
