"""ONLY interface other modules/apps may import from the payments module."""
from modules.payments.internal.payment_service import PaymentService

class PaymentsPublicApi:
    def __init__(self):
        self._svc = PaymentService()

    def authorize(self, order_id: str, amount_cents: int, token: str) -> dict:
        return self._svc.authorize(order_id, amount_cents, token)

    def capture(self, payment_id: str) -> dict | None:
        return self._svc.capture(payment_id)

    def refund(self, payment_id: str, amount_cents: int) -> dict | None:
        return self._svc.refund(payment_id, amount_cents)
