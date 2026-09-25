# E-Commerce Polyglot Language Shootout

A modular-monolith e-commerce backend implemented **11 times** — once per
language — behind one contract (gRPC/protobuf), fronted by one modern
TypeScript/React storefront, so you can directly compare Python, Rust, Go,
C++, Java, C#, Kotlin, Scala, R, Ruby, and OCaml under identical load.

> **Honesty note on scope.** This repo gives every backend a real, working
> HTTP surface (`/api/perf`, `/healthz`) built with each language's modern
> toolchain, a correct modular-monolith folder layout
> (`public-api / internal / infrastructure`), a working Dockerfile, and a
> shared `.proto` contract for `OrdersService` / `PaymentsService` /
> `NotificationsService`. The **business logic depth** (full checkout,
> payments, catalog, search) is intentionally concentrated in the shared
> proto contract and the frontend rather than duplicated at full e-commerce
> depth in all 11 languages — that's a multi-week build, not a single
> generated deliverable. Treat each `backend-*` as the correct scaffold to
> build out, not a finished storefront.

---

## 1. Project Synopsis

```
┌──────────────────────────────────────────────────────────────────────┐
│                         frontend-web (TanStack Start)                │
│         React 19 · shadcn/ui · Tailwind v4 · TanStack Query/Router   │
│               Zustand · Zod · Drizzle ORM · Stripe · Motion          │
└───────────────────────────────┬──────────────────────────────────────┘
                                │  gRPC / HTTP, one backend at a time
                                ▼
        ┌───────────────────────────────────────────────────────┐
        │        ONE of 11 interchangeable backends             │
        │  python · rust · golang · cpp · java · csharp ·       │
        │  kotlin · scala · r · ruby · ocaml                    │
        │                                                       │
        │  apps/web-api/            single deployable entry     │
        │  modules/orders/          public-api | internal |     │
        │  modules/payments/                     infrastructure │
        │  modules/notifications/                               │
        └───────────────────────────────────────────────────────┘
                                │
                                ▼
        ┌──────────────────────────────────────────────────────┐
        │   shared-benchmarks/  (k6 load-test.js + parser)     │
        │   Prometheus  ──remote-write──> Grafana dashboards   │
        └──────────────────────────────────────────────────────┘
```

Every module (`orders`, `payments`, `notifications`) exposes **only** its
`public-api` package to the rest of the system. `internal/` holds business
logic and is never imported outside the module; `infrastructure/` holds
persistence and is never imported outside the module either. This is the
"modular monolith" pattern: one deployable process per backend, hard
module boundaries enforced by folder + import convention (and, in
strongly-typed languages, by package/visibility modifiers).

## 2. Directory Structure

```
├── .github/workflows/ci.yml     Cross-platform (Win11 + Ubuntu) CI pipeline
├── compose.yml                  Local orchestration: all 11 backends + frontend + telemetry
├── nx.json / package.json       Nx workspace root
├── proto/ecommerce.proto        Shared gRPC contract: Orders/Payments/Notifications/Perf
├── frontend-web/                TanStack Start (React 19) storefront
├── shared-benchmarks/           k6 load test + results parser + Prometheus/Grafana config
├── benchmarking-results/        Gitignored output: <backend>-report.json + summary.md
├── k8s/                         base/ manifests + overlays/{aws,azure,gcp}
├── backend-python/              uv (pyproject.toml) · FastAPI
├── backend-rust/                Cargo · axum
├── backend-golang/              go build · net/http
├── backend-cpp/                 CMake · cpp-httplib
├── backend-java/                Maven · Spring Boot
├── backend-csharp/               MSBuild (dotnet) · ASP.NET Core minimal API
├── backend-kotlin/              Gradle (Kotlin DSL) · Ktor
├── backend-scala/               sbt · Pekko HTTP
├── backend-r/                   devtools/usethis (DESCRIPTION) · plumber
├── backend-ruby/                Rake (Gemfile) · Sinatra + Puma
└── backend-ocaml/               Dune · Dream
```

Each `backend-*/` follows the identical internal layout:

```
backend-<lang>/
├── apps/web-api/           # single deployable entry point
├── modules/orders/{public-api,internal,infrastructure}
├── modules/payments/{public-api,internal,infrastructure}
├── modules/notifications/{public-api,internal,infrastructure}
├── Dockerfile
└── <native build file>     # pyproject.toml, Cargo.toml, go.mod, CMakeLists.txt,
                             # pom.xml, .csproj, build.gradle.kts, build.sbt,
                             # DESCRIPTION, Gemfile+Rakefile, dune-project
```

## 3. How the Comparison Result Is Generated

This is the process from `shared-benchmarks/`, wired end-to-end:

1. **Build** — `npx nx affected -t build` compiles whichever backends
   changed, using each language's native tool (uv, Cargo, `go build`,
   CMake, Maven, MSBuild, Gradle, sbt, devtools, Rake, Dune).
2. **Containerize** — `npx nx affected -t container-build` runs
   `docker build` for each backend's `Dockerfile`.
3. **Isolate & load-test** — for each backend in turn, `compose.yml` boots
   *only that backend* plus Prometheus/Grafana, waits for warm-up, then
   `shared-benchmarks/load-test.js` (a k6 script) drives a ramping
   30-VU load against `GET /api/perf` for 60s total and writes
   `benchmarking-results/<backend>-report.json`. The container is torn
   down before the next backend starts, so no two backends ever compete
   for host CPU at the same time.
4. **Aggregate** — `shared-benchmarks/parse-results.js` reads every
   `*-report.json` and writes `benchmarking-results/summary.md`, a single
   Markdown table of total requests, req/s, p95 latency, and failure rate
   per language.
5. **Publish** — in CI, that summary is appended to the GitHub Actions
   step summary and the full JSON reports are uploaded as a workflow
   artifact.

## 4. Prerequisites

| Tool | Why |
|---|---|
| Node.js 22+ | Nx workspace, frontend, k6 result parsing |
| Docker Desktop (Win11) / Docker Engine + Compose plugin (Ubuntu) | container build + compose orchestration |
| k6 | load generation (`choco install k6` / `apt install k6` — see CI workflow for the exact repo setup) |
| Per-language toolchains, only if building **outside** Docker | uv, Rust, Go 1.23+, CMake+a C++23 compiler, JDK 21+ & Maven, .NET 9 SDK, Gradle, sbt, R 4.4+, Ruby 3.3+, OCaml 5.2+/opam/dune |
| `kubectl` + `kustomize` | cloud deployment only |

You do **not** need all 11 language toolchains installed locally if you're
only running via Docker — Docker builds each language's official image.

## 5. Run Locally — Windows 11

```powershell
# 1. Install prerequisites once
choco install nodejs docker-desktop k6 -y
# (Docker Desktop needs a manual restart/WSL2 backend enable on first install)

# 2. Install workspace dependencies
npm ci

# 3. Build everything Nx knows about (skips anything already cached)
npx nx run-many -t build --all

# 4. Bring up the full stack (frontend + all 11 backends + telemetry)
docker compose up -d --build

# 5. Open the storefront
start http://localhost:3000
# Grafana: http://localhost:3001   Prometheus: http://localhost:9090

# 6. Tear down
docker compose down
```

## 6. Run Locally — Ubuntu

```bash
# 1. Install prerequisites once
sudo apt-get update && sudo apt-get install -y nodejs npm docker-compose-plugin
curl -s https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /usr/share/keyrings/k6-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install -y k6

# 2. Install workspace dependencies
npm ci

# 3. Build everything
npx nx run-many -t build --all

# 4. Bring up the full stack
docker compose up -d --build

# 5. Open the storefront
xdg-open http://localhost:3000   # Grafana: :3001, Prometheus: :9090

# 6. Tear down
docker compose down
```

## 7. Run the Benchmark Shootout (Local, Either OS)

```bash
mkdir -p benchmarking-results

# One backend at a time (repeat the port/label pairs below per backend):
docker compose up -d prometheus grafana backend-rust
sleep 5
TARGET_URL=http://localhost:8081 BACKEND_NAME=rust npx nx run shared-benchmarks:k6-test
docker compose down

# ...repeat for backend-python:8080/python, backend-golang:8082/golang,
# backend-cpp:8083/cpp, backend-java:8084/java, backend-csharp:8085/csharp,
# backend-kotlin:8086/kotlin, backend-scala:8087/scala, backend-r:8088/r,
# backend-ruby:8089/ruby, backend-ocaml:8090/ocaml

# Then aggregate:
npx nx run shared-benchmarks:analyze
cat benchmarking-results/summary.md
```

`.github/workflows/ci.yml` runs this exact loop automatically on Ubuntu
runners (see below) — copy its `Run sequential benchmarking matrix` step
if you want a single script instead of repeating the block above by hand.

## 8. Run via Docker Only (No Local Toolchains)

Every backend and the frontend build entirely inside Docker — you never
need Rust, the JVM, .NET, R, or OCaml installed on the host:

```bash
docker compose build          # builds all 12 images
docker compose up -d          # runs all 12 containers + telemetry
docker compose down           # stop everything
```

## 9. Deploy to the Cloud (AWS / Azure / GCP)

The `k8s/` folder uses Kustomize: `base/` holds the platform-neutral
manifests, `overlays/<cloud>/` rewrites image references to that cloud's
registry.

```bash
# 1. Build & push images to your registry (example: AWS ECR)
aws ecr get-login-password | docker login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com
docker compose build
for svc in frontend-web backend-python backend-rust backend-golang backend-cpp \
           backend-java backend-csharp backend-kotlin backend-scala backend-r \
           backend-ruby backend-ocaml; do
  docker tag "$svc:latest" "<account>.dkr.ecr.<region>.amazonaws.com/$svc:latest"
  docker push "<account>.dkr.ecr.<region>.amazonaws.com/$svc:latest"
done

# 2. Edit k8s/overlays/aws/kustomization.yaml to match your registry/account,
#    then apply:
kubectl apply -k k8s/overlays/aws

# Azure: same pattern with `az acr login` and k8s/overlays/azure
# GCP:   same pattern with `gcloud auth configure-docker` and k8s/overlays/gcp
```

`k8s/base/backend-python-deployment.yaml` is the reference shape (readiness
probe, resource requests/limits, ClusterIP service). Copy it for each of
the other ten backends, changing only the name/image/labels — they're
intentionally identical so the cluster-side comparison stays apples-to-apples.

## 10. Trigger via GitHub Actions

Push to `main` or open a PR — `.github/workflows/ci.yml` runs automatically
across an `ubuntu-latest` **and** a `windows-latest` runner in parallel:

- Both runners install Node, the language toolchains, and run
  `nx affected -t build` so compile correctness is checked on both OSes —
  with one exception: `backend-ocaml` builds on Ubuntu only. Installing its
  opam dependency tree on Windows hits a well-documented Win32 MAX_PATH
  (260-char) limitation in `dune`'s own upstream package (it bundles a
  deeply nested test fixture, and GitHub Actions' Windows checkout path
  combined with opam's switch paths pushes past 260 characters). This is
  an upstream/OS characteristic, not something fixable from this repo —
  Windows CI explicitly excludes it (`--exclude=backend-ocaml`) rather than
  silently failing.
- The Docker build, the full sequential k6 benchmark matrix, and the
  results aggregation run **only on the Ubuntu runner** — Windows runners
  don't reliably support `docker compose` with Linux containers, so
  duplicating the benchmark there would be flaky rather than useful. This
  is a deliberate scoping choice, not an oversight.
- Results land in the **Summary** tab of the workflow run (Markdown table)
  and as a downloadable `benchmarking-metrics-ubuntu-latest` artifact
  containing every `*-report.json` plus `summary.md`.

To trigger manually: `gh workflow run ci.yml` (requires the GitHub CLI and
repo write access), or use the **Run workflow** button under the Actions
tab if `workflow_dispatch` is enabled for your fork.

## 11. Module Boundary Rules (enforced by convention)

- Only `modules/<name>/public-api/**` (or its per-language equivalent:
  `publicapi`, `PublicApi`) may be imported by `apps/web-api` or by another
  module.
- `modules/<name>/internal/**` contains business logic and is never
  imported from outside `modules/<name>/`.
- `modules/<name>/infrastructure/**` contains persistence/repo code and is
  never imported from outside `modules/<name>/`.
- The `proto/ecommerce.proto` file is the single source of truth for
  cross-module and frontend↔backend contracts; regenerate per-language
  stubs from it rather than hand-editing generated code.

## 12. Second-Pass Additions (formerly out of scope)

These were explicitly out of scope in the first pass and are now
implemented for real across all 11 backends:

- **Catalog module** — a fourth module (`modules/catalog/`) alongside
  orders/payments/notifications, in the proto contract and in every
  backend: seed data, `list`/`get`/`search`, and live
  `GET /api/products[...]` endpoints. See section 2's directory listing.
- **Real Stripe webhook verification** — HMAC-SHA256 + timestamp-tolerance
  signature verification, implemented natively in each language's own
  crypto primitives (not left as pseudo-code). Full algorithm and the
  per-language file map are in `docs/stripe-webhooks.md`.
- **gRPC codegen wired into Nx** — `buf.yaml`/`buf.gen.yaml` generate
  stubs for Python/Go/Java/Kotlin/C#/C++/TypeScript on every `build`;
  Rust/Scala/R/Ruby/OCaml generate natively via their own toolchain
  (`build.rs`+tonic-build, sbt-protoc, RProtoBuf, a Rake `:codegen` task,
  and a dune rule respectively). See each backend's `generate` Nx target.
- **Autoscaling, secrets, service mesh** — `k8s/base/hpa.yaml`,
  `k8s/base/secrets.yaml` (placeholder values — wire to your cloud's
  secret manager, never commit real ones), and `k8s/base/service-mesh.yaml`
  (Istio strict mTLS + sidecar injection).

## 13. Third-Pass Additions

- **Payment capture/refund + order-state transitions** — every backend now
  has a real state machine (`PENDING → PAID/CANCELLED → REFUNDED` for
  orders, `AUTHORIZED → CAPTURED → REFUNDED` for payments), and every
  Stripe webhook handler parses the verified event's `type` and dispatches
  to these transitions instead of just returning `{"received": true}`.
- **HPA/K8s deployment copied to all 11 backends** — `k8s/base/` now has a
  `backend-<lang>-deployment.yaml` + `backend-<lang>-hpa.yaml` pair for
  every backend (not just the one Python reference), all wired into
  `kustomization.yaml`, and the AWS/Azure/GCP overlays rewrite images for
  all 11 backends + the frontend.
- **Real gRPC servers for Python, Go, and Java** — full servicer
  implementations of all 4 proto services, wired to run alongside each
  backend's HTTP server. See `docs/grpc-coverage.md` for exactly which
  backends have this and which still don't, and why — I don't have
  network access to execute `buf generate` in this environment, so I only
  claim a gRPC server is done where I've hand-written the full handler
  layer against that language's real generated-code shape, not where I'd
  be guessing.

## 14. Still Genuinely Out of Scope

- Full recommendation engines / production auth (session management, SSO)
- gRPC servers for the other 8 backends (Rust, C#, Kotlin, C++, Scala,
  Ruby, OCaml, R) — codegen is wired for all of them, but the
  servicer/handler layer itself isn't written yet. `docs/grpc-coverage.md`
  has a per-language status table and the steps to finish each one; R in
  particular has no mature async gRPC *server* story and would realistically
  need a sidecar rather than an in-process implementation.
