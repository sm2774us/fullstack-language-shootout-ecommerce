# syntax=docker/dockerfile:1
# `rust:1-slim` floats to the latest stable Rust 1.x release rather than a
# pinned patch version. This project pinned rust:1.82-slim initially, which
# went stale once a transitive dependency (indexmap) started requiring
# Cargo's edition2024 support (stabilized in Rust 1.85) — floating avoids
# repeating that class of failure as the crates.io ecosystem moves forward.
FROM rust:1-slim AS builder
WORKDIR /app
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
COPY --from=builder /app/target/release/backend-rust /usr/local/bin/backend-rust
EXPOSE 8080
CMD ["backend-rust"]
