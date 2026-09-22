# syntax=docker/dockerfile:1
FROM golang:1.23-bookworm AS builder
WORKDIR /app
COPY go.mod ./
COPY . .
RUN go build -o /backend-golang ./apps/web-api

FROM gcr.io/distroless/base-debian12
COPY --from=builder /backend-golang /backend-golang
EXPOSE 8080 50051
CMD ["/backend-golang"]
