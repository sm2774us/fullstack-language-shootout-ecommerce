// ONLY interface other modules/apps may import from the orders module.
package com.example.modules.orders.publicapi

import com.example.modules.orders.internal.{Order, OrderService}

class OrdersPublicApi:
  private val svc = new OrderService
  def createOrder(orderId: String, totalCents: Long): Order = svc.createOrder(orderId, totalCents)
  def getOrder(orderId: String): Option[Order] = svc.getOrder(orderId)
  def markPaid(orderId: String, paymentId: String): Option[Order] = svc.markPaid(orderId, paymentId)
  def markFailed(orderId: String, reason: String): Option[Order] = svc.markFailed(orderId, reason)
  def markRefunded(orderId: String, amountCents: Long): Option[Order] = svc.markRefunded(orderId, amountCents)
