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

    // NOTE: an earlier attempt to tune Netty's engine (requestQueueLimit,
    // callGroupSize, etc. via a `configure` block) failed to compile against
    // this project's actual Ktor version — the compiler's own error listed
    // the real available overloads, and none combine `port`/`configure`
    // together the way various Ktor doc versions suggest. Rather than keep
    // guessing at an API surface with no compiler here to check it against,
    // this uses the plain (factory, port, host, module) call, which is
    // exactly one of the compiler-confirmed real candidates. If Kotlin's
    // http_req_failed threshold trips again under the benchmark's 30 VUs,
    // address it via the k6/workflow side (docs/grpc-coverage.md-style
    // honesty applies here too) rather than further unverified engine tuning.
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
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
