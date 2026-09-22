# syntax=docker/dockerfile:1
FROM ubuntu:24.04 AS builder
RUN apt-get update && apt-get install -y build-essential cmake git ca-certificates libssl-dev && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
RUN cmake -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build -j"$(nproc)"

FROM ubuntu:24.04
RUN apt-get update && apt-get install -y --no-install-recommends libstdc++6 && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/build/backend_cpp /usr/local/bin/backend_cpp
EXPOSE 8080
CMD ["backend_cpp"]
