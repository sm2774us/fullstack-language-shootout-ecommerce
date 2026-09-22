// ONLY interface other modules/apps may import from the orders module.
package com.example.modules.orders.publicapi

import com.example.modules.orders.internal.OrderService

class OrdersPublicApi {
    private val svc = OrderService()
    fun createOrder(orderId: String, totalCents: Long) = svc.createOrder(orderId, totalCents)
    fun getOrder(orderId: String) = svc.getOrder(orderId)
    fun markPaid(orderId: String, paymentId: String) = svc.markPaid(orderId, paymentId)
    fun markFailed(orderId: String, reason: String) = svc.markFailed(orderId, reason)
    fun markRefunded(orderId: String, amountCents: Long) = svc.markRefunded(orderId, amountCents)
}
