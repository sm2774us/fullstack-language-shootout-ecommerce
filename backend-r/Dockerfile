# syntax=docker/dockerfile:1
FROM rocker/r-ver:4.4.2
RUN R -e "install.packages(c('plumber','jsonlite','uuid','devtools'), repos='https://cloud.r-project.org')"
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["Rscript", "apps/web-api/run.R"]
