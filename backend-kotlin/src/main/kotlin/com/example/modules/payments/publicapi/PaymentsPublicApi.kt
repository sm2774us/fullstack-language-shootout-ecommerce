// ONLY interface other modules/apps may import from the payments module.
package com.example.modules.payments.publicapi

import com.example.modules.payments.internal.PaymentService

class PaymentsPublicApi {
    private val svc = PaymentService()
    fun authorize(paymentId: String, orderId: String, amountCents: Long) = svc.authorize(paymentId, orderId, amountCents)
    fun capture(paymentId: String) = svc.capture(paymentId)
    fun refund(paymentId: String) = svc.refund(paymentId)
}
