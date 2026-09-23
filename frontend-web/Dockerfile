# syntax=docker/dockerfile:1
#
# Build context for this Dockerfile is the REPO ROOT, not frontend-web/ —
# see compose.yml (`build: { context: ., dockerfile: frontend-web/Dockerfile }`)
# and the frontend-web:container-build Nx target in package.json. This is
# required so npm ci can see the root package-lock.json and install the
# exact, verified dependency versions instead of npm re-resolving live
# against whatever's newly published on the npm registry today, which is
# what caused this backend's real ERESOLVE peer-dependency failure.
FROM node:22-slim AS builder
WORKDIR /app
COPY package.json package-lock.json ./
COPY frontend-web/package.json ./frontend-web/package.json
COPY shared-benchmarks/package.json ./shared-benchmarks/package.json
RUN npm ci --workspace=frontend-web --include-workspace-root=true
COPY frontend-web ./frontend-web
WORKDIR /app/frontend-web
RUN npm run build

FROM node:22-slim
WORKDIR /app
COPY --from=builder /app/frontend-web/.output ./.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
