// Business logic — hidden from the outside world.
package com.example.modules.payments.internal

import java.util.concurrent.ConcurrentHashMap

final case class Payment(paymentId: String, orderId: String, var status: String, amountCents: Long)

class PaymentService:
  private val store = new ConcurrentHashMap[String, Payment]()

  def authorize(paymentId: String, orderId: String, amountCents: Long): Payment =
    val p = Payment(paymentId, orderId, "AUTHORIZED", amountCents)
    store.put(paymentId, p)
    p

  def capture(paymentId: String): Option[Payment] =
    Option(store.get(paymentId)).filter(_.status == "AUTHORIZED").map { p => p.status = "CAPTURED"; p }

  def refund(paymentId: String): Option[Payment] =
    Option(store.get(paymentId)).filter(_.status == "CAPTURED").map { p => p.status = "REFUNDED"; p }
