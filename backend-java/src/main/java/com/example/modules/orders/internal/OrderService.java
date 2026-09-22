// Business logic — hidden from the outside world.
package com.example.modules.orders.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService {
    public static class Order {
        public String orderId, status, paymentId, cancelReason;
        public long totalCents, refundedCents;
        public Order(String orderId, long totalCents, String status) {
            this.orderId = orderId; this.totalCents = totalCents; this.status = status;
        }
    }

    private static final Map<String, Order> STORE = new ConcurrentHashMap<>();

    public Order createOrder(String orderId, long totalCents) {
        Order o = new Order(orderId, totalCents, "PENDING");
        STORE.put(orderId, o);
        return o;
    }

    public Optional<Order> getOrder(String orderId) { return Optional.ofNullable(STORE.get(orderId)); }

    public Optional<Order> markPaid(String orderId, String paymentId) {
        Order o = STORE.get(orderId);
        if (o == null) return Optional.empty();
        o.status = "PAID"; o.paymentId = paymentId;
        return Optional.of(o);
    }

    public Optional<Order> markFailed(String orderId, String reason) {
        Order o = STORE.get(orderId);
        if (o == null) return Optional.empty();
        o.status = "CANCELLED"; o.cancelReason = reason;
        return Optional.of(o);
    }

    public Optional<Order> markRefunded(String orderId, long amountCents) {
        Order o = STORE.get(orderId);
        if (o == null || !"PAID".equals(o.status)) return Optional.empty();
        o.status = "REFUNDED"; o.refundedCents = amountCents;
        return Optional.of(o);
    }
}
