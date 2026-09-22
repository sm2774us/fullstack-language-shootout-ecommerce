# syntax=docker/dockerfile:1
FROM rust:1.82-slim AS builder
WORKDIR /app
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
COPY --from=builder /app/target/release/backend-rust /usr/local/bin/backend-rust
EXPOSE 8080
CMD ["backend-rust"]
