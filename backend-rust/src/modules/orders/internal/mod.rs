//! Business logic — hidden from the outside world, not `pub` beyond this module tree.
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;
use uuid::Uuid;

#[derive(Serialize, Deserialize, Clone)]
pub struct Order {
    pub order_id: String,
    pub total_cents: i64,
    pub status: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payment_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancel_reason: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refunded_cents: Option<i64>,
}

static ORDERS: Mutex<Option<HashMap<String, Order>>> = Mutex::new(None);

fn with_store<T>(f: impl FnOnce(&mut HashMap<String, Order>) -> T) -> T {
    let mut guard = ORDERS.lock().unwrap();
    let map = guard.get_or_insert_with(HashMap::new);
    f(map)
}

pub fn create_order(total_cents: i64) -> Order {
    let order = Order {
        order_id: Uuid::new_v4().to_string(),
        total_cents,
        status: "PENDING".into(),
        payment_id: None,
        cancel_reason: None,
        refunded_cents: None,
    };
    with_store(|m| m.insert(order.order_id.clone(), order.clone()));
    order
}

pub fn get_order(order_id: &str) -> Option<Order> {
    with_store(|m| m.get(order_id).cloned())
}

/// Transitions PENDING -> PAID. Called only after Stripe signature verification.
pub fn mark_paid(order_id: &str, payment_id: &str) -> Option<Order> {
    with_store(|m| {
        let o = m.get_mut(order_id)?;
        o.status = "PAID".into();
        o.payment_id = Some(payment_id.to_string());
        Some(o.clone())
    })
}

/// Transitions PENDING -> CANCELLED.
pub fn mark_failed(order_id: &str, reason: &str) -> Option<Order> {
    with_store(|m| {
        let o = m.get_mut(order_id)?;
        o.status = "CANCELLED".into();
        o.cancel_reason = Some(reason.to_string());
        Some(o.clone())
    })
}

/// Transitions PAID -> REFUNDED.
pub fn mark_refunded(order_id: &str, amount_cents: i64) -> Option<Order> {
    with_store(|m| {
        let o = m.get_mut(order_id)?;
        if o.status != "PAID" { return None; }
        o.status = "REFUNDED".into();
        o.refunded_cents = Some(amount_cents);
        Some(o.clone())
    })
}
