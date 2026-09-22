// Business logic — hidden from the outside world.
package com.example.modules.orders.internal

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Order(
    val orderId: String, val totalCents: Long, var status: String,
    var paymentId: String? = null, var cancelReason: String? = null, var refundedCents: Long? = null
)

class OrderService {
    private val store = ConcurrentHashMap<String, Order>()

    fun createOrder(orderId: String, totalCents: Long): Order {
        val o = Order(orderId, totalCents, "PENDING")
        store[orderId] = o
        return o
    }

    fun getOrder(orderId: String): Order? = store[orderId]

    fun markPaid(orderId: String, paymentId: String): Order? {
        val o = store[orderId] ?: return null
        o.status = "PAID"; o.paymentId = paymentId
        return o
    }

    fun markFailed(orderId: String, reason: String): Order? {
        val o = store[orderId] ?: return null
        o.status = "CANCELLED"; o.cancelReason = reason
        return o
    }

    fun markRefunded(orderId: String, amountCents: Long): Order? {
        val o = store[orderId] ?: return null
        if (o.status != "PAID") return null
        o.status = "REFUNDED"; o.refundedCents = amountCents
        return o
    }
}
