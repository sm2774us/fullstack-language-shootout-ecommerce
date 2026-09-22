// Business logic — hidden from the outside world.
package com.example.modules.orders.internal

import java.util.concurrent.ConcurrentHashMap

final case class Order(
  orderId: String, totalCents: Long, var status: String,
  var paymentId: Option[String] = None, var cancelReason: Option[String] = None, var refundedCents: Option[Long] = None
)

class OrderService:
  private val store = new ConcurrentHashMap[String, Order]()

  def createOrder(orderId: String, totalCents: Long): Order =
    val o = Order(orderId, totalCents, "PENDING")
    store.put(orderId, o)
    o

  def getOrder(orderId: String): Option[Order] = Option(store.get(orderId))

  def markPaid(orderId: String, paymentId: String): Option[Order] =
    Option(store.get(orderId)).map { o => o.status = "PAID"; o.paymentId = Some(paymentId); o }

  def markFailed(orderId: String, reason: String): Option[Order] =
    Option(store.get(orderId)).map { o => o.status = "CANCELLED"; o.cancelReason = Some(reason); o }

  def markRefunded(orderId: String, amountCents: Long): Option[Order] =
    Option(store.get(orderId)).filter(_.status == "PAID").map { o =>
      o.status = "REFUNDED"; o.refundedCents = Some(amountCents); o
    }
