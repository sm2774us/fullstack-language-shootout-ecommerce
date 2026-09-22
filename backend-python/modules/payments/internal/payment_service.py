"""Stripe-backed authorization/capture/refund logic — internal only."""
import uuid


class PaymentService:
    _payments: dict = {}

    def authorize(self, order_id: str, amount_cents: int, token: str) -> dict:
        # In production: call Stripe PaymentIntents.create(capture_method="manual").
        payment_id = str(uuid.uuid4())
        payment = {"payment_id": payment_id, "order_id": order_id,
                   "status": "AUTHORIZED", "amount_cents": amount_cents}
        self._payments[payment_id] = payment
        return payment

    def capture(self, payment_id: str) -> dict | None:
        # In production: call Stripe PaymentIntents.capture(payment_id).
        payment = self._payments.get(payment_id)
        if payment is None or payment["status"] != "AUTHORIZED":
            return None
        payment["status"] = "CAPTURED"
        return payment

    def refund(self, payment_id: str, amount_cents: int) -> dict | None:
        # In production: call Stripe Refunds.create(payment_intent=payment_id, amount=amount_cents).
        payment = self._payments.get(payment_id)
        if payment is None or payment["status"] != "CAPTURED":
            return None
        payment["status"] = "REFUNDED"
        payment["refunded_cents"] = amount_cents
        return payment
