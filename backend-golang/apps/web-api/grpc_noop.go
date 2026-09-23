//go:build !grpc

// Default (no `-tags grpc`) build of the gRPC entry point: a no-op, so
// main.go's `go serveGRPC("50051")` call still compiles and runs without
// requiring modules/generated (the buf-generate output) to exist. See
// grpc_server.go for the real implementation and its build tag, and
// docs/grpc-coverage.md for the full picture across all 11 backends.
package main

import "log"

func serveGRPC(port string) {
	log.Printf("[grpc] skipping gRPC server on :%s — generated stubs not built in "+
		"(rebuild with `go build -tags grpc` after running "+
		"`npx nx run backend-golang:generate`). Serving HTTP only.", port)
}
