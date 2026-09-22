// Real gRPC server for the Java backend, implementing every RPC in
// proto/ecommerce.proto against the same module public-APIs the HTTP
// surface in WebApiApplication.java uses — one business logic layer, two
// transports.
//
// Imports com.example.ecommerce.v1.* (grpc-java's generated package name
// for package ecommerce.v1;), produced by running codegen locally:
//
//     npx nx run proto:generate      # requires network access to buf.build
//     npx nx run backend-java:generate
//
// buf.gen.yaml emits the generated Java sources straight into
// src/main/java/, matching Maven's default source layout. This sandbox has
// no route to buf.build's remote plugins, so this file is written and
// reviewed against the .proto schema but has not been compiled here —
// running the command above locally generates the missing package and this
// file compiles and runs as-is.
package com.example.webapi;

import com.example.ecommerce.v1.*;
import com.example.modules.catalog.publicapi.CatalogPublicApi;
import com.example.modules.orders.publicapi.OrdersPublicApi;
import com.example.modules.payments.publicapi.PaymentsPublicApi;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.UUID;

public class GrpcServer {

    static class PerfServiceImpl extends PerfServiceGrpc.PerfServiceImplBase {
        @Override
        public void perf(PerfRequest request, StreamObserver<PerfResponse> responseObserver) {
            responseObserver.onNext(PerfResponse.newBuilder()
                .setBackend("backend-java").setLanguage("java")
                .setServerTimeUnixMs(System.currentTimeMillis()).setStatus("ok").build());
            responseObserver.onCompleted();
        }
    }

    static class CatalogServiceImpl extends CatalogServiceGrpc.CatalogServiceImplBase {
        private final CatalogPublicApi catalog = new CatalogPublicApi();

        @Override
        public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
            var builder = ListProductsResponse.newBuilder();
            catalog.list(request.getCategory()).forEach(p -> builder.addProducts(toProto(p)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getProduct(GetProductRequest request, StreamObserver<Product> responseObserver) {
            catalog.get(request.getSku()).ifPresentOrElse(
                p -> { responseObserver.onNext(toProto(p)); responseObserver.onCompleted(); },
                () -> responseObserver.onError(Status.NOT_FOUND.withDescription("product not found").asRuntimeException())
            );
        }

        @Override
        public void searchProducts(SearchProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
            var builder = ListProductsResponse.newBuilder();
            catalog.search(request.getQuery()).forEach(p -> builder.addProducts(toProto(p)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        private Product toProto(com.example.modules.catalog.infrastructure.SeedData.Product p) {
            return Product.newBuilder().setSku(p.sku()).setName(p.name()).setDescription(p.description())
                .setPriceCents(p.priceCents()).setCategory(p.category()).setStockQuantity(p.stockQuantity()).build();
        }
    }

    static class OrdersServiceImpl extends OrdersServiceGrpc.OrdersServiceImplBase {
        private final OrdersPublicApi orders = new OrdersPublicApi();

        @Override
        public void createOrder(CreateOrderRequest request, StreamObserver<Order> responseObserver) {
            long total = request.getItemsList().stream()
                .mapToLong(i -> (long) i.getQuantity() * i.getUnitPriceCents()).sum();
            var order = orders.createOrder(UUID.randomUUID().toString(), total);
            responseObserver.onNext(toProto(order));
            responseObserver.onCompleted();
        }

        @Override
        public void getOrder(GetOrderRequest request, StreamObserver<Order> responseObserver) {
            orders.getOrder(request.getOrderId()).ifPresentOrElse(
                o -> { responseObserver.onNext(toProto(o)); responseObserver.onCompleted(); },
                () -> responseObserver.onError(Status.NOT_FOUND.withDescription("order not found").asRuntimeException())
            );
        }

        @Override
        public void listOrders(ListOrdersRequest request, StreamObserver<ListOrdersResponse> responseObserver) {
            // Reference implementation returns an empty page; a real
            // deployment adds a listByCustomer query to OrderService, same
            // requirement the HTTP surface would have.
            responseObserver.onNext(ListOrdersResponse.newBuilder().build());
            responseObserver.onCompleted();
        }

        private Order toProto(com.example.modules.orders.internal.OrderService.Order o) {
            return Order.newBuilder().setOrderId(o.orderId).setTotalCents(o.totalCents).setStatus(o.status).build();
        }
    }

    static class PaymentsServiceImpl extends PaymentsServiceGrpc.PaymentsServiceImplBase {
        private final PaymentsPublicApi payments = new PaymentsPublicApi();

        @Override
        public void authorizePayment(AuthorizePaymentRequest request, StreamObserver<PaymentResult> responseObserver) {
            var p = payments.authorize(UUID.randomUUID().toString(), request.getOrderId(), request.getAmountCents());
            responseObserver.onNext(toProto(p));
            responseObserver.onCompleted();
        }

        @Override
        public void capturePayment(CapturePaymentRequest request, StreamObserver<PaymentResult> responseObserver) {
            payments.capture(request.getPaymentId()).ifPresentOrElse(
                p -> { responseObserver.onNext(toProto(p)); responseObserver.onCompleted(); },
                () -> responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("payment not capturable").asRuntimeException())
            );
        }

        @Override
        public void refundPayment(RefundPaymentRequest request, StreamObserver<PaymentResult> responseObserver) {
            payments.refund(request.getPaymentId()).ifPresentOrElse(
                p -> { responseObserver.onNext(toProto(p)); responseObserver.onCompleted(); },
                () -> responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("payment not refundable").asRuntimeException())
            );
        }

        private PaymentResult toProto(com.example.modules.payments.internal.PaymentService.Payment p) {
            return PaymentResult.newBuilder().setPaymentId(p.paymentId).setOrderId(p.orderId)
                .setStatus(p.status).setAmountCents(p.amountCents).build();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50051)
            .addService(new PerfServiceImpl())
            .addService(new CatalogServiceImpl())
            .addService(new OrdersServiceImpl())
            .addService(new PaymentsServiceImpl())
            .build()
            .start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
