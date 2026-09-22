// Single deployable entry point for the Scala backend.
package com.example.webapi

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import com.example.modules.catalog.publicapi.CatalogPublicApi
import com.example.modules.catalog.infrastructure.Product
import com.example.modules.orders.publicapi.OrdersPublicApi
import com.example.modules.payments.publicapi.PaymentsPublicApi
import com.example.modules.payments.internal.StripeWebhookVerifier
import spray.json.*
import scala.concurrent.ExecutionContextExecutor

object ProductJson extends DefaultJsonProtocol:
  given RootJsonFormat[Product] = jsonFormat6(Product.apply)
import ProductJson.given

object Main:
  def main(args: Array[String]): Unit =
    given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "backend-scala")
    given ExecutionContextExecutor = system.executionContext

    val catalog = new CatalogPublicApi
    val orders = new OrdersPublicApi
    val payments = new PaymentsPublicApi

    val route =
      path("api" / "perf") {
        get {
          val body = JsObject(
            "backend" -> JsString("backend-scala"),
            "language" -> JsString("scala"),
            "server_time_unix_ms" -> JsNumber(System.currentTimeMillis()),
            "status" -> JsString("ok")
          )
          complete(body.prettyPrint)
        }
      } ~ path("healthz") {
        get { complete("""{"status":"ok"}""") }
      } ~ path("api" / "products") {
        get {
          parameter("category".optional) { category =>
            complete(catalog.list(category).toJson.prettyPrint)
          }
        }
      } ~ path("api" / "products" / "search") {
        get {
          parameter("q") { q => complete(catalog.search(q).toJson.prettyPrint) }
        }
      } ~ path("api" / "products" / Segment) { sku =>
        get {
          catalog.get(sku) match
            case Some(p) => complete(p.toJson.prettyPrint)
            case None    => complete("""{"error":"not found"}""")
        }
      } ~ path("webhooks" / "stripe") {
        post {
          headerValueByName("Stripe-Signature") { sig =>
            entity(as[String]) { payload =>
              StripeWebhookVerifier.verify(payload, sig, sys.env.get("STRIPE_WEBHOOK_SECRET")) match
                case Left(err) => complete(JsObject("error" -> JsString(err)).prettyPrint)
                case Right(_) =>
                  val event = payload.parseJson.asJsObject
                  val eventType = event.fields.get("type").collect { case JsString(s) => s }.getOrElse("")
                  val dataObj = event.fields.get("data").map(_.asJsObject)
                    .flatMap(_.fields.get("object")).map(_.asJsObject)
                  val orderId = dataObj.flatMap(_.fields.get("metadata")).map(_.asJsObject)
                    .flatMap(_.fields.get("order_id")).collect { case JsString(s) => s }.getOrElse("")
                  val paymentIntentId = dataObj.flatMap(_.fields.get("id")).collect { case JsString(s) => s }.getOrElse("")

                  eventType match
                    case "payment_intent.succeeded" if orderId.nonEmpty =>
                      payments.capture(paymentIntentId)
                      orders.markPaid(orderId, paymentIntentId)
                    case "payment_intent.payment_failed" if orderId.nonEmpty =>
                      val reason = dataObj.flatMap(_.fields.get("last_payment_error")).map(_.asJsObject)
                        .flatMap(_.fields.get("message")).collect { case JsString(s) => s }.getOrElse("payment failed")
                      orders.markFailed(orderId, reason)
                    case "charge.refunded" if orderId.nonEmpty =>
                      val amount = dataObj.flatMap(_.fields.get("amount_refunded")).collect { case JsNumber(n) => n.toLong }.getOrElse(0L)
                      orders.markRefunded(orderId, amount)
                    case _ => ()

                  complete(JsObject("received" -> JsBoolean(true), "event_type" -> JsString(eventType)).prettyPrint)
            }
          }
        }
      }

    Http().newServerAt("0.0.0.0", 8080).bind(route)
