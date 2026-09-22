// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance) — mirrors backend-python's webhook_handler.py.
package com.example.modules.payments.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

public class StripeWebhookVerifier {
    private static final long TOLERANCE_SECONDS = 300;

    public static boolean verify(String payload, String sigHeader, String secret) throws Exception {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is not configured");

        long timestamp = 0;
        java.util.List<String> sigs = new java.util.ArrayList<>();
        for (String part : sigHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].trim().equals("t")) timestamp = Long.parseLong(kv[1].trim());
            else if (kv[0].trim().equals("v1")) sigs.add(kv[1].trim());
        }
        if (timestamp == 0 || sigs.isEmpty()) throw new IllegalArgumentException("malformed Stripe-Signature header");

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > TOLERANCE_SECONDS) throw new IllegalArgumentException("timestamp outside tolerance");

        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        return sigs.stream().anyMatch(s -> java.security.MessageDigest.isEqual(
            s.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8)));
    }
}
