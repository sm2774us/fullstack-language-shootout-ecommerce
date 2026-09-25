# syntax=docker/dockerfile:1
FROM rocker/r-ver:4.4.2
# httpuv (plumber's own HTTP engine dependency) and sodium (a plumber
# Import) compile C code needing these headers — rocker/r-ver is a
# minimal base image and doesn't include them. devtools deliberately is
# NOT installed here: nothing the running container invokes uses it
# (apps/web-api/run.R only calls plumber::plumb()) — it's this project's
# designated LOCAL development tool (see package.json's `generate` target,
# which does use it, run by a developer with a full system toolchain, not
# inside this slim runtime image). Installing it here dragged in its own
# enormous dependency tree (usethis, pkgdown, roxygen2, rcmdcheck, and
# their own system requirements like pandoc and libgit2) for zero runtime
# benefit, and that tree's own failures were masking the real, fixable
# problem: plumber's own dependencies needed these system libraries.
RUN apt-get update && apt-get install -y --no-install-recommends \
    libssl-dev libsodium-dev zlib1g-dev libcurl4-openssl-dev \
    libprotobuf-dev protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*
RUN R -e "install.packages(c('plumber','jsonlite','uuid','digest','RProtoBuf'), repos='https://cloud.r-project.org')"
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["Rscript", "apps/web-api/run.R"]
