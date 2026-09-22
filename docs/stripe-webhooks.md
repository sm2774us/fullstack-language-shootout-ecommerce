# Stripe Webhook Verification — Shared Pattern

Every backend that terminates a Stripe webhook (`POST /webhooks/stripe`)
implements the same algorithm natively, without depending on the `stripe`
SDK where one isn't idiomatic for the language, so the verification logic
is fully auditable:

1. Split the `Stripe-Signature` header into `t=<unix timestamp>` and one or
   more `v1=<hex hmac>` values (Stripe rotates signing secrets, so more
   than one `v1` can be present during rotation).
2. Reject if the timestamp is more than 300 seconds from "now" — this is
   the replay-attack guard.
3. Recompute `HMAC-SHA256(webhook_signing_secret, "{timestamp}.{raw_body}")`
   and hex-encode it.
4. Compare the recomputed signature against every `v1` value using a
   constant-time comparison (never `==`/`.equals`), accepting if any match.
5. Only once verified, parse `raw_body` as the event JSON and act on it.

This exact sequence is implemented in:

| Backend | File | Crypto primitive used |
|---|---|---|
| Python | `backend-python/modules/payments/internal/webhook_handler.py` | stdlib `hmac` + `hashlib` |
| Rust | `backend-rust/src/modules/payments/internal/webhook.rs` | `hmac` + `sha2` crates |
| Go | `backend-golang/modules/payments/internal/webhook.go` | stdlib `crypto/hmac` |
| C++ | `backend-cpp/modules/payments/internal/webhook.hpp` | OpenSSL `HMAC()` |
| Java | `backend-java/.../payments/internal/StripeWebhookVerifier.java` | `javax.crypto.Mac` |
| C# | `backend-csharp/modules/payments/Internal/StripeWebhookVerifier.cs` | `System.Security.Cryptography.HMACSHA256` |
| Kotlin | `backend-kotlin/.../payments/internal/StripeWebhookVerifier.kt` | `javax.crypto.Mac` |
| Scala | `backend-scala/.../payments/internal/StripeWebhookVerifier.scala` | `javax.crypto.Mac` |
| R | `backend-r/R/payments_webhook.R` | `digest::hmac` |
| Ruby | `backend-ruby/modules/payments/internal/stripe_webhook_verifier.rb` | stdlib `OpenSSL::HMAC` |
| OCaml | `backend-ocaml/modules/payments/internal/stripe_webhook_verifier.ml` | `digestif` |
| Frontend (TS) | `frontend-web/src/routes/webhooks/stripe.ts` | official `stripe` SDK's `constructEvent` (same algorithm, vendor-verified) |

## Configuration

Every backend and the frontend read the signing secret from the
`STRIPE_WEBHOOK_SECRET` environment variable — see
`k8s/base/secrets.yaml` for how it's supplied in cluster, and
`compose.yml` / your local `.env` for local runs. Never commit a real
value; the manifests here reference a Kubernetes Secret by name only.

## What's still a stub

Payment *capture*, *refund*, and order-state transitions on a verified
webhook event are left as a single `// In production: ...` comment in
each handler — wiring those requires the Orders module's public-api to
expose a `MarkOrderPaid`/`MarkOrderFailed` call, which is a natural next
addition once you've picked a primary implementation language.
