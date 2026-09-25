# syntax=docker/dockerfile:1
FROM ruby:3.3-slim
RUN apt-get update && apt-get install -y build-essential && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY Gemfile ./
RUN bundle config set --local without 'development' && bundle install
COPY . .
EXPOSE 8080
CMD ["bundle", "exec", "puma", "-b", "tcp://0.0.0.0:8080", "apps/web-api/config.ru"]
