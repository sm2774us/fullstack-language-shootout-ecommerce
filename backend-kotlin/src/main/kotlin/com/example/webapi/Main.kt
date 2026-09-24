// Single deployable entry point for the Kotlin backend.
package com.example.webapi

import com.example.modules.catalog.publicapi.CatalogPublicApi
import com.example.modules.orders.publicapi.OrdersPublicApi
import com.example.modules.payments.publicapi.PaymentsPublicApi
import com.example.modules.payments.internal.StripeWebhookVerifier
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

fun main() {
    val catalog = CatalogPublicApi()
    val orders = OrdersPublicApi()
    val payments = PaymentsPublicApi()

    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        configure = {
            // Ktor's Netty engine defaults derive callGroupSize/workerGroupSize/
            // connectionGroupSize from Runtime.getRuntime().availableProcessors(),
            // and defaults requestQueueLimit to 16. On GitHub Actions' constrained
            // (2 vCPU) runners those defaults are too small for this project's k6
            // benchmark (30 concurrent VUs sustained for 60s), causing the
            // request queue to fill and connections to fail/time out under load
            // — explicit, load-appropriate values instead of CPU-count guessing.
            requestQueueLimit = 200
            runningLimit = 100
            callGroupSize = 16
            workerGroupSize = 16
            connectionGroupSize = 8
        }
    ) {
        install(ContentNegotiation) { json() }
        routing {
            get("/api/perf") {
                call.respond(
                    mapOf(
                        "backend" to "backend-kotlin",
                        "language" to "kotlin",
                        "server_time_unix_ms" to System.currentTimeMillis(),
                        "status" to "ok"
                    )
                )
            }
            get("/healthz") { call.respond(mapOf("status" to "ok")) }

            get("/api/products") {
                call.respond(catalog.list(call.request.queryParameters["category"]))
            }
            get("/api/products/search") {
                call.respond(catalog.search(call.request.queryParameters["q"] ?: ""))
            }
            get("/api/products/{sku}") {
                val p = catalog.get(call.parameters["sku"] ?: "")
                call.respond(p ?: mapOf("error" to "not found"))
            }
            post("/webhooks/stripe") {
                val payload = call.receiveText()
                val sig = call.request.headers["Stripe-Signature"] ?: ""
                val (ok, error) = StripeWebhookVerifier.verify(payload, sig, System.getenv("STRIPE_WEBHOOK_SECRET"))
                if (!ok) {
                    call.respond(mapOf("error" to (error ?: "unknown")))
                    return@post
                }
                val event = Json.parseToJsonElement(payload).jsonObject
                val eventType = event["type"]?.jsonPrimitive?.contentOrNull ?: ""
                val dataObj = event["data"]?.jsonObject?.get("object")?.jsonObject
                val orderId = dataObj?.get("metadata")?.jsonObject?.get("order_id")?.jsonPrimitive?.contentOrNull ?: ""
                val paymentIntentId = dataObj?.get("id")?.jsonPrimitive?.contentOrNull ?: ""

                when (eventType) {
                    "payment_intent.succeeded" -> if (orderId.isNotEmpty()) {
                        payments.capture(paymentIntentId)
                        orders.markPaid(orderId, paymentIntentId)
                    }
                    "payment_intent.payment_failed" -> if (orderId.isNotEmpty()) {
                        val reason = dataObj?.get("last_payment_error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "payment failed"
                        orders.markFailed(orderId, reason)
                    }
                    "charge.refunded" -> if (orderId.isNotEmpty()) {
                        val amount = dataObj?.get("amount_refunded")?.jsonPrimitive?.longOrNull ?: 0
                        orders.markRefunded(orderId, amount)
                    }
                }
                call.respond(mapOf("received" to true, "event_type" to eventType))
            }
        }
    }.start(wait = true)
}
