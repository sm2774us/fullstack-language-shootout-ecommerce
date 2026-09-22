// ONLY interface other modules/apps may import from the payments module.
package com.example.modules.payments.publicapi

import com.example.modules.payments.internal.{Payment, PaymentService}

class PaymentsPublicApi:
  private val svc = new PaymentService
  def authorize(paymentId: String, orderId: String, amountCents: Long): Payment = svc.authorize(paymentId, orderId, amountCents)
  def capture(paymentId: String): Option[Payment] = svc.capture(paymentId)
  def refund(paymentId: String): Option[Payment] = svc.refund(paymentId)
