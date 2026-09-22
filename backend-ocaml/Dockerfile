# syntax=docker/dockerfile:1
FROM ocaml/opam:ubuntu-24.04-ocaml-5.2 AS builder
WORKDIR /app
USER opam
RUN opam install -y dune dream yojson
COPY --chown=opam . .
RUN opam exec -- dune build --release

FROM ubuntu:24.04
RUN apt-get update && apt-get install -y --no-install-recommends libev4 ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/_build/default/bin/main.exe /usr/local/bin/backend-ocaml
EXPOSE 8080
CMD ["backend-ocaml"]
