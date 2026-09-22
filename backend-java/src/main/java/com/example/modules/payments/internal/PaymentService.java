// Business logic — hidden from the outside world.
package com.example.modules.payments.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentService {
    public static class Payment {
        public String paymentId, orderId, status;
        public long amountCents;
        public Payment(String paymentId, String orderId, String status, long amountCents) {
            this.paymentId = paymentId; this.orderId = orderId; this.status = status; this.amountCents = amountCents;
        }
    }

    private static final Map<String, Payment> STORE = new ConcurrentHashMap<>();

    public Payment authorize(String paymentId, String orderId, long amountCents) {
        Payment p = new Payment(paymentId, orderId, "AUTHORIZED", amountCents);
        STORE.put(paymentId, p);
        return p;
    }

    public Optional<Payment> capture(String paymentId) {
        Payment p = STORE.get(paymentId);
        if (p == null || !"AUTHORIZED".equals(p.status)) return Optional.empty();
        p.status = "CAPTURED";
        return Optional.of(p);
    }

    public Optional<Payment> refund(String paymentId) {
        Payment p = STORE.get(paymentId);
        if (p == null || !"CAPTURED".equals(p.status)) return Optional.empty();
        p.status = "REFUNDED";
        return Optional.of(p);
    }
}
