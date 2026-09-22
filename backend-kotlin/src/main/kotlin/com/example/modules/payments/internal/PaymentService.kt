// Business logic — hidden from the outside world.
package com.example.modules.payments.internal

import java.util.concurrent.ConcurrentHashMap

data class Payment(val paymentId: String, val orderId: String, var status: String, val amountCents: Long)

class PaymentService {
    private val store = ConcurrentHashMap<String, Payment>()

    fun authorize(paymentId: String, orderId: String, amountCents: Long): Payment {
        val p = Payment(paymentId, orderId, "AUTHORIZED", amountCents)
        store[paymentId] = p
        return p
    }

    fun capture(paymentId: String): Payment? {
        val p = store[paymentId] ?: return null
        if (p.status != "AUTHORIZED") return null
        p.status = "CAPTURED"
        return p
    }

    fun refund(paymentId: String): Payment? {
        val p = store[paymentId] ?: return null
        if (p.status != "CAPTURED") return null
        p.status = "REFUNDED"
        return p
    }
}
