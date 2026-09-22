// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance) — mirrors backend-python's webhook_handler.py.
package com.example.modules.payments.internal

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object StripeWebhookVerifier {
    private const val toleranceSeconds = 300L

    fun verify(payload: String, sigHeader: String, secret: String?): Pair<Boolean, String?> {
        if (secret.isNullOrBlank()) return false to "STRIPE_WEBHOOK_SECRET is not configured"

        var timestamp = 0L
        val sigs = mutableListOf<String>()
        for (part in sigHeader.split(",")) {
            val kv = part.split("=", limit = 2)
            if (kv.size != 2) continue
            when (kv[0].trim()) {
                "t" -> timestamp = kv[1].trim().toLongOrNull() ?: 0
                "v1" -> sigs.add(kv[1].trim())
            }
        }
        if (timestamp == 0L || sigs.isEmpty()) return false to "malformed Stripe-Signature header"

        val now = System.currentTimeMillis() / 1000
        if (Math.abs(now - timestamp) > toleranceSeconds) return false to "timestamp outside tolerance"

        val signedPayload = "$timestamp.$payload"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal(signedPayload.toByteArray()).joinToString("") { "%02x".format(it) }

        return if (sigs.any { MessageDigest.isEqual(it.toByteArray(), expected.toByteArray()) })
            true to null else false to "signature mismatch"
    }
}
