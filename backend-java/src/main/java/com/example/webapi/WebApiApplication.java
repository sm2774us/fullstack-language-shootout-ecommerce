// Single deployable entry point for the Java backend.
package com.example.webapi;

import com.example.modules.catalog.publicapi.CatalogPublicApi;
import com.example.modules.payments.internal.StripeWebhookVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebApiApplication {
    public static void main(String[] args) throws Exception {
        startGrpcServerIfAvailable(args);
        SpringApplication.run(WebApiApplication.class, args);
    }

    // GrpcServer is excluded from the default Maven build (see pom.xml's
    // maven-compiler-plugin excludes) because it references grpc-java
    // codegen output that doesn't exist until `buf generate` has run — see
    // docs/grpc-coverage.md. Looking it up reflectively, rather than
    // calling GrpcServer.main(args) directly, avoids a hard compile-time
    // dependency on a class that may not have been compiled in.
    private static void startGrpcServerIfAvailable(String[] args) {
        Thread grpcThread = new Thread(() -> {
            try {
                Class<?> grpcServerClass = Class.forName("com.example.webapi.GrpcServer");
                var mainMethod = grpcServerClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) args);
            } catch (ClassNotFoundException e) {
                System.out.println("[grpc] skipping gRPC server — GrpcServer was not built in "
                    + "(run `npx nx run backend-java:generate` then rebuild without the "
                    + "GrpcServer.java compiler exclude in pom.xml to enable it). "
                    + "Serving HTTP only.");
            } catch (Exception e) {
                throw new RuntimeException("gRPC server failed to start", e);
            }
        });
        grpcThread.setDaemon(true);
        grpcThread.start();
    }
}

@RestController
class PerfController {
    @GetMapping("/api/perf")
    public Map<String, Object> perf() {
        return Map.of(
            "backend", "backend-java",
            "language", "java",
            "server_time_unix_ms", System.currentTimeMillis(),
            "status", "ok"
        );
    }

    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }
}

@RestController
class CatalogController {
    private final CatalogPublicApi catalog = new CatalogPublicApi();

    @GetMapping("/api/products")
    public Object list(@RequestParam(required = false) String category) {
        return catalog.list(category);
    }

    @GetMapping("/api/products/search")
    public Object search(@RequestParam String q) {
        return catalog.search(q);
    }

    @GetMapping("/api/products/{sku}")
    public Object get(@PathVariable String sku) {
        return catalog.get(sku).<Object>map(p -> p).orElse(Map.of("error", "not found"));
    }
}

@RestController
class StripeWebhookController {
    private final com.example.modules.orders.publicapi.OrdersPublicApi orders = new com.example.modules.orders.publicapi.OrdersPublicApi();
    private final com.example.modules.payments.publicapi.PaymentsPublicApi payments = new com.example.modules.payments.publicapi.PaymentsPublicApi();
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @PostMapping("/webhooks/stripe")
    public Map<String, Object> webhook(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String sig) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean ok = StripeWebhookVerifier.verify(payload, sig, System.getenv("STRIPE_WEBHOOK_SECRET"));
            if (!ok) {
                result.put("error", "signature mismatch");
                return result;
            }
            var event = mapper.readTree(payload);
            String eventType = event.path("type").asText("");
            var dataObj = event.path("data").path("object");
            String orderId = dataObj.path("metadata").path("order_id").asText("");
            String paymentIntentId = dataObj.path("id").asText("");

            switch (eventType) {
                case "payment_intent.succeeded" -> {
                    if (!orderId.isEmpty()) {
                        payments.capture(paymentIntentId);
                        orders.markPaid(orderId, paymentIntentId);
                    }
                }
                case "payment_intent.payment_failed" -> {
                    if (!orderId.isEmpty()) {
                        String reason = dataObj.path("last_payment_error").path("message").asText("payment failed");
                        orders.markFailed(orderId, reason);
                    }
                }
                case "charge.refunded" -> {
                    if (!orderId.isEmpty()) {
                        long amount = dataObj.path("amount_refunded").asLong(0);
                        orders.markRefunded(orderId, amount);
                    }
                }
                default -> {}
            }
            result.put("received", true);
            result.put("event_type", eventType);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}
