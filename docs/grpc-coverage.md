# gRPC Coverage — What's Actually Wired

Every backend serves the shootout's `/api/perf` and catalog/webhook HTTP
endpoints. This file tracks the separate question: which backends also run
a real **gRPC** server implementing `proto/ecommerce.proto`'s
`PerfService`/`CatalogService`/`OrdersService`/`PaymentsService`.

## Why this isn't uniform across all 11

Building this repo, I do not have network access to `buf.build`'s remote
plugins, so I cannot *execute* `buf generate` to produce verified
`ecommerce_pb2.py`, `ecommerce.pb.go`, generated Java sources, etc. I can
still write the servicer/handler layer that sits on top of that generated
code — and did, for the three backends below — but I won't claim a gRPC
server is "done" for a language unless I've written the full handler
layer against the exact generated-code shape that language's official
gRPC tooling produces. Where I haven't done that yet, the backend stays
HTTP-only rather than shipping something unverified and mislabeled.

## Status

| Backend | gRPC server | File | Notes |
|---|---|---|---|
| Python | ✅ Implemented, degrades gracefully | `apps/web-api/grpc_server.py` | All 4 services; runs on port 50051 alongside FastAPI on 8080. If `modules/generated/` isn't present, `main.py` catches the `ImportError`, logs it, and serves HTTP-only — it does not crash the process. |
| Go | ✅ Implemented, excluded from default build | `apps/web-api/grpc_server.go` | All 4 services. Gated behind the `grpc` build tag (`go build -tags grpc ./apps/web-api`) so the DEFAULT build (what the Dockerfile runs) compiles without `modules/generated` existing — `apps/web-api/grpc_noop.go` supplies a no-op `serveGRPC` for that default path. |
| Java | ✅ Implemented, excluded from default build | `src/main/java/com/example/webapi/GrpcServer.java` | All 4 services. Excluded from Maven's default compilation via `pom.xml`'s `maven-compiler-plugin` excludes, since it references grpc-java codegen output that doesn't exist yet. `WebApiApplication.java` looks it up reflectively rather than calling it directly, so the app compiles and runs HTTP-only without it. |
| Rust | ⏳ Codegen wired, servicer not written | `build.rs` (tonic-build) | `tonic::include_proto!` macro is the next step — natural fit given axum is already async |
| C# | ⏳ Codegen wired, servicer not written | `buf.gen.yaml` → `modules/generated` | ASP.NET Core has first-class `Grpc.AspNetCore` support; straightforward next addition |
| Kotlin | ⏳ Codegen wired, servicer not written | `buf.gen.yaml` → `modules/generated` | Can reuse Java's generated message classes + `grpc-kotlin` coroutine stubs |
| C++ | ⏳ Codegen wired, servicer not written | `buf.gen.yaml` → `modules/generated` | `grpc++` service boilerplate is the most verbose of any language here; higher risk to hand-write unverified |
| Scala | ⏳ Codegen wired, servicer not written | `build.sbt` (scalapb) | Akka gRPC or scalapb-grpc would host this |
| Ruby | ⏳ Codegen wired, servicer not written | `Rakefile :codegen` | The `grpc` gem is already in the Gemfile; needs `GRPC::RpcServer` wiring |
| OCaml | ⏳ Codegen wired, servicer not written | `modules/generated/dune` | `grpc-lwt` is genuinely low-level for hand-written, unexecuted code — highest-risk remaining target |
| R | ⏳ Codegen wired (descriptors only), servicer not practical | `R/generated.R` (RProtoBuf) | R has no mature async gRPC *server* story; `RProtoBuf` gives you message (de)serialization, not a service host. A real R gRPC server would realistically wrap a C++/Go sidecar rather than run in-process — that's a design decision for whoever picks R, not a gap to silently paper over here |

## How to finish one of the ⏳ backends

1. Run codegen for that backend: `npx nx run backend-<lang>:generate`
   (network access to `buf.build` required — or the language's own native
   protoc plugin for Rust/Scala/R/Ruby/OCaml, see `buf.gen.yaml`'s comments).
2. Write a servicer/handler class implementing each RPC method, calling
   the same `modules/<name>/public-api` functions the HTTP surface already
   uses — see `backend-python/apps/web-api/grpc_server.py` or
   `backend-golang/apps/web-api/grpc_server.go` as the reference shape.
3. Start the gRPC server alongside the existing HTTP server (both examples
   above run it in a background thread/goroutine on port 50051).
4. Add `"50051:50051"` (or the next free host port) to that service in
   `compose.yml` and `EXPOSE 50051` to its `Dockerfile`.
