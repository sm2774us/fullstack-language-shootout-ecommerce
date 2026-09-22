"""Real Stripe webhook signature verification, following Stripe's documented
construct-event algorithm (https://stripe.com/docs/webhooks#verify-manually)
without depending on the `stripe` SDK, so the logic is auditable in full.

Every backend that terminates Stripe webhooks directly (Python here; Go and
the TanStack Start frontend have their own copies) implements exactly this
algorithm: split the `Stripe-Signature` header into timestamp + v1 signature,
recompute HMAC-SHA256 over "{timestamp}.{payload}" using the webhook signing
secret, compare in constant time, and reject anything older than the
configured tolerance to block replay attacks.
"""
import hashlib
import hmac
import json
import os
import time

STRIPE_WEBHOOK_SECRET = os.environ.get("STRIPE_WEBHOOK_SECRET", "")
DEFAULT_TOLERANCE_SECONDS = 300


class StripeSignatureError(Exception):
    pass


def _parse_signature_header(sig_header: str) -> tuple[int, list[str]]:
    timestamp = None
    signatures = []
    for part in sig_header.split(","):
        if "=" not in part:
            continue
        key, _, value = part.partition("=")
        key = key.strip()
        if key == "t":
            timestamp = int(value)
        elif key == "v1":
            signatures.append(value)
    if timestamp is None or not signatures:
        raise StripeSignatureError("malformed Stripe-Signature header")
    return timestamp, signatures


def verify_and_parse_webhook(payload: bytes, sig_header: str,
                              tolerance_seconds: int = DEFAULT_TOLERANCE_SECONDS) -> dict:
    """Verifies `payload` against `sig_header` using STRIPE_WEBHOOK_SECRET.
    Raises StripeSignatureError on any failure. Returns the parsed event
    dict only once the signature and timestamp both check out.
    """
    if not STRIPE_WEBHOOK_SECRET:
        raise StripeSignatureError("STRIPE_WEBHOOK_SECRET is not configured")

    timestamp, signatures = _parse_signature_header(sig_header)

    now = int(time.time())
    if abs(now - timestamp) > tolerance_seconds:
        raise StripeSignatureError("timestamp outside tolerance — possible replay")

    signed_payload = f"{timestamp}.{payload.decode('utf-8')}".encode("utf-8")
    expected_sig = hmac.new(
        STRIPE_WEBHOOK_SECRET.encode("utf-8"), signed_payload, hashlib.sha256
    ).hexdigest()

    if not any(hmac.compare_digest(expected_sig, sig) for sig in signatures):
        raise StripeSignatureError("signature mismatch")

    return json.loads(payload)
