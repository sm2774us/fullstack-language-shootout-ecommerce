# syntax=docker/dockerfile:1
FROM ocaml/opam:ubuntu-24.04-ocaml-5.2 AS builder
WORKDIR /app
USER opam
# digestif: used by the Stripe webhook verifier (modules/payments/internal).
# ocaml-protoc-plugin: used by the codegen rule in modules/generated/dune,
# which participates in the default `dune build` target. grpc/grpc-lwt are
# deliberately left out — declared in backend-ocaml.opam as a future
# dependency (see docs/grpc-coverage.md) but not yet referenced by any
# buildable code, so there's no reason to install them here yet.
RUN opam install -y dune dream yojson digestif ocaml-protoc-plugin
COPY --chown=opam . .
RUN opam exec -- dune build --release

FROM ubuntu:24.04
RUN apt-get update && apt-get install -y --no-install-recommends libev4 ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/_build/default/bin/main.exe /usr/local/bin/backend-ocaml
EXPOSE 8080
CMD ["backend-ocaml"]
