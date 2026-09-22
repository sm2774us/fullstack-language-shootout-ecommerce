// ONLY interface other modules/apps may import from the orders module.
package com.example.modules.orders.publicapi;

import com.example.modules.orders.internal.OrderService;
import java.util.Optional;

public class OrdersPublicApi {
    private final OrderService svc = new OrderService();
    public OrderService.Order createOrder(String orderId, long totalCents) { return svc.createOrder(orderId, totalCents); }
    public Optional<OrderService.Order> getOrder(String orderId) { return svc.getOrder(orderId); }
    public Optional<OrderService.Order> markPaid(String orderId, String paymentId) { return svc.markPaid(orderId, paymentId); }
    public Optional<OrderService.Order> markFailed(String orderId, String reason) { return svc.markFailed(orderId, reason); }
    public Optional<OrderService.Order> markRefunded(String orderId, long amountCents) { return svc.markRefunded(orderId, amountCents); }
}
