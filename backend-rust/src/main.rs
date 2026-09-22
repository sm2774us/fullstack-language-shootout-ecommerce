//! Single deployable entry point (apps/web-api equivalent) for the Rust backend.
mod modules;

use axum::{
    extract::{Path, Query, State},
    routing::{get, post},
    Json, Router,
};
use modules::catalog::public_api::CatalogPublicApi;
use modules::payments::internal::webhook::verify_stripe_webhook;
use serde_json::json;
use std::{
    collections::HashMap,
    sync::Arc,
    time::{SystemTime, UNIX_EPOCH},
};

struct AppState {
    catalog: CatalogPublicApi,
}

async fn list_products(State(state): State<Arc<AppState>>, Query(params): Query<HashMap<String, String>>) -> Json<serde_json::Value> {
    let category = params.get("category").map(|s| s.as_str());
    Json(json!(state.catalog.list(category)))
}

async fn get_product(State(state): State<Arc<AppState>>, Path(sku): Path<String>) -> Json<serde_json::Value> {
    match state.catalog.get(&sku) {
        Some(p) => Json(json!(p)),
        None => Json(json!({"error": "not found"})),
    }
}

async fn search_products(State(state): State<Arc<AppState>>, Query(params): Query<HashMap<String, String>>) -> Json<serde_json::Value> {
    let q = params.get("q").map(|s| s.as_str()).unwrap_or("");
    Json(json!(state.catalog.search(q)))
}

async fn stripe_webhook(headers: axum::http::HeaderMap, body: axum::body::Bytes) -> Json<serde_json::Value> {
    let sig = headers.get("stripe-signature").and_then(|v| v.to_str().ok()).unwrap_or("");
    let secret = std::env::var("STRIPE_WEBHOOK_SECRET").unwrap_or_default();
    if let Err(e) = verify_stripe_webhook(&body, sig, &secret) {
        return Json(json!({"error": e}));
    }

    let event: serde_json::Value = match serde_json::from_slice(&body) {
        Ok(v) => v,
        Err(_) => return Json(json!({"error": "malformed event JSON"})),
    };
    let event_type = event["type"].as_str().unwrap_or("").to_string();
    let data_obj = &event["data"]["object"];
    let order_id = data_obj["metadata"]["order_id"].as_str().unwrap_or("");
    let payment_intent_id = data_obj["id"].as_str().unwrap_or("");

    match event_type.as_str() {
        "payment_intent.succeeded" if !order_id.is_empty() => {
            modules::payments::public_api::capture(payment_intent_id);
            modules::orders::public_api::mark_paid(order_id, payment_intent_id);
        }
        "payment_intent.payment_failed" if !order_id.is_empty() => {
            let reason = data_obj["last_payment_error"]["message"].as_str().unwrap_or("payment failed");
            modules::orders::public_api::mark_failed(order_id, reason);
        }
        "charge.refunded" if !order_id.is_empty() => {
            let amount = data_obj["amount_refunded"].as_i64().unwrap_or(0);
            modules::orders::public_api::mark_refunded(order_id, amount);
        }
        _ => {}
    }

    Json(json!({"received": true, "event_type": event_type}))
}

async fn perf() -> Json<serde_json::Value> {
    let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis() as i64;
    Json(json!({
        "backend": "backend-rust",
        "language": "rust",
        "server_time_unix_ms": now,
        "status": "ok"
    }))
}

async fn healthz() -> Json<serde_json::Value> { Json(json!({"status": "ok"})) }

#[tokio::main]
async fn main() {
    let state = Arc::new(AppState { catalog: CatalogPublicApi::new() });
    let app = Router::new()
        .route("/api/perf", get(perf))
        .route("/healthz", get(healthz))
        .route("/api/products", get(list_products))
        .route("/api/products/search", get(search_products))
        .route("/api/products/:sku", get(get_product))
        .route("/webhooks/stripe", post(stripe_webhook))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
