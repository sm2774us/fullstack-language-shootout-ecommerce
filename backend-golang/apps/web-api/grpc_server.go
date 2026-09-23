//go:build grpc

// Real gRPC server for the Go backend, implementing every RPC in
// proto/ecommerce.proto against the same module publicapi packages the
// HTTP surface in main.go uses — one business logic layer, two transports.
//
// Excluded from the DEFAULT build via the `grpc` build tag above: this
// file imports modules/generated, produced by running codegen locally:
//
//	npx nx run proto:generate     # requires network access to buf.build
//	# or, backend-golang specifically:
//	npx nx run backend-golang:generate
//
// buf.gen.yaml at the repo root emits ecommerce.pb.go + ecommerce_grpc.pb.go
// into modules/generated/. This sandbox has no route to buf.build's remote
// plugins, so this file is written and reviewed against the .proto schema
// but has not been compiled here. Without codegen having run, that package
// has zero .go files, so this file would fail to compile if included in
// the default build — hence the build tag. Once codegen has run locally,
// build with `go build -tags grpc ./apps/web-api` to include it, and see
// docs/grpc-coverage.md for the full picture across all 11 backends.
package main

import (
	"context"
	"log"
	"net"
	"time"

	catalogapi "github.com/example/ecommerce-shootout/backend-golang/modules/catalog/publicapi"
	generated "github.com/example/ecommerce-shootout/backend-golang/modules/generated"
	ordersapi "github.com/example/ecommerce-shootout/backend-golang/modules/orders/publicapi"
	paymentsapi "github.com/example/ecommerce-shootout/backend-golang/modules/payments/publicapi"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type perfServer struct{ generated.UnimplementedPerfServiceServer }

func (s *perfServer) Perf(ctx context.Context, req *generated.PerfRequest) (*generated.PerfResponse, error) {
	return &generated.PerfResponse{
		Backend:           "backend-golang",
		Language:          "golang",
		ServerTimeUnixMs:  time.Now().UnixMilli(),
		Status:            "ok",
	}, nil
}

type catalogServer struct {
	generated.UnimplementedCatalogServiceServer
	catalog *catalogapi.CatalogPublicApi
}

func (s *catalogServer) ListProducts(ctx context.Context, req *generated.ListProductsRequest) (*generated.ListProductsResponse, error) {
	products := s.catalog.List(req.Category)
	out := make([]*generated.Product, 0, len(products))
	for _, p := range products {
		out = append(out, productToProto(p))
	}
	return &generated.ListProductsResponse{Products: out}, nil
}

func (s *catalogServer) GetProduct(ctx context.Context, req *generated.GetProductRequest) (*generated.Product, error) {
	p, ok := s.catalog.Get(req.Sku)
	if !ok {
		return nil, status.Errorf(codes.NotFound, "product %s not found", req.Sku)
	}
	return productToProto(p), nil
}

func (s *catalogServer) SearchProducts(ctx context.Context, req *generated.SearchProductsRequest) (*generated.ListProductsResponse, error) {
	products := s.catalog.Search(req.Query)
	out := make([]*generated.Product, 0, len(products))
	for _, p := range products {
		out = append(out, productToProto(p))
	}
	return &generated.ListProductsResponse{Products: out}, nil
}

type ordersServer struct {
	generated.UnimplementedOrdersServiceServer
}

func (s *ordersServer) CreateOrder(ctx context.Context, req *generated.CreateOrderRequest) (*generated.Order, error) {
	var total int64
	for _, item := range req.Items {
		total += int64(item.Quantity) * item.UnitPriceCents
	}
	order := ordersapi.CreateOrder(generateOrderID(), total)
	return orderToProto(order), nil
}

func (s *ordersServer) GetOrder(ctx context.Context, req *generated.GetOrderRequest) (*generated.Order, error) {
	o, ok := ordersapi.GetOrder(req.OrderId)
	if !ok {
		return nil, status.Errorf(codes.NotFound, "order %s not found", req.OrderId)
	}
	return orderToProto(o), nil
}

func (s *ordersServer) ListOrders(ctx context.Context, req *generated.ListOrdersRequest) (*generated.ListOrdersResponse, error) {
	// Reference implementation returns an empty page; a real deployment
	// adds a ListByCustomer query to the orders module, same requirement
	// the HTTP surface would have.
	return &generated.ListOrdersResponse{Orders: nil, NextPageToken: ""}, nil
}

type paymentsServer struct {
	generated.UnimplementedPaymentsServiceServer
}

func (s *paymentsServer) AuthorizePayment(ctx context.Context, req *generated.AuthorizePaymentRequest) (*generated.PaymentResult, error) {
	p := paymentsapi.Authorize(generatePaymentID(), req.OrderId, req.AmountCents)
	return paymentToProto(p), nil
}

func (s *paymentsServer) CapturePayment(ctx context.Context, req *generated.CapturePaymentRequest) (*generated.PaymentResult, error) {
	p, ok := paymentsapi.Capture(req.PaymentId)
	if !ok {
		return nil, status.Errorf(codes.FailedPrecondition, "payment not capturable")
	}
	return paymentToProto(p), nil
}

func (s *paymentsServer) RefundPayment(ctx context.Context, req *generated.RefundPaymentRequest) (*generated.PaymentResult, error) {
	p, ok := paymentsapi.Refund(req.PaymentId)
	if !ok {
		return nil, status.Errorf(codes.FailedPrecondition, "payment not refundable")
	}
	return paymentToProto(p), nil
}

func productToProto(p catalogapi.Product) *generated.Product {
	return &generated.Product{
		Sku: p.SKU, Name: p.Name, Description: p.Description,
		PriceCents: p.PriceCents, Category: p.Category,
		StockQuantity: int32(p.Stock), ImageUrls: p.ImageURLs,
	}
}

func orderToProto(o ordersapi.Order) *generated.Order {
	return &generated.Order{
		OrderId: o.OrderID, TotalCents: o.Total, Status: o.Status,
		CreatedAtUnixMs: o.CreatedAt,
	}
}

func paymentToProto(p paymentsapi.Payment) *generated.PaymentResult {
	return &generated.PaymentResult{
		PaymentId: p.PaymentID, OrderId: p.OrderID, Status: p.Status, AmountCents: p.AmountCents,
	}
}

func serveGRPC(port string) {
	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		log.Fatalf("grpc listen failed: %v", err)
	}
	grpcServer := grpc.NewServer()
	generated.RegisterPerfServiceServer(grpcServer, &perfServer{})
	generated.RegisterCatalogServiceServer(grpcServer, &catalogServer{catalog: catalogapi.NewCatalogPublicApi()})
	generated.RegisterOrdersServiceServer(grpcServer, &ordersServer{})
	generated.RegisterPaymentsServiceServer(grpcServer, &paymentsServer{})
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("grpc serve failed: %v", err)
	}
}
