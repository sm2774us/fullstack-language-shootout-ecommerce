# syntax=docker/dockerfile:1
FROM python:3.13-slim AS base
WORKDIR /app
RUN pip install --no-cache-dir uv
COPY pyproject.toml ./
RUN uv pip install --system --no-cache -e .
COPY . .
EXPOSE 8080 50051
CMD ["python", "apps/web-api/main.py"]
