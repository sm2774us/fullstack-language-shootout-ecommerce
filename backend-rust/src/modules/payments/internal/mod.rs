//! Business logic — hidden from the outside world.
pub mod webhook;

use serde::Serialize;
use std::collections::HashMap;
use std::sync::Mutex;

#[derive(Serialize, Clone)]
pub struct Payment { pub payment_id: String, pub order_id: String, pub status: String, pub amount_cents: i64 }

static PAYMENTS: Mutex<Option<HashMap<String, Payment>>> = Mutex::new(None);
fn with_store<T>(f: impl FnOnce(&mut HashMap<String, Payment>) -> T) -> T {
    let mut guard = PAYMENTS.lock().unwrap();
    let map = guard.get_or_insert_with(HashMap::new);
    f(map)
}

pub fn authorize(payment_id: &str, order_id: &str, amount_cents: i64) -> Payment {
    let p = Payment { payment_id: payment_id.into(), order_id: order_id.into(), status: "AUTHORIZED".into(), amount_cents };
    with_store(|m| m.insert(p.payment_id.clone(), p.clone()));
    p
}
pub fn capture(payment_id: &str) -> Option<Payment> {
    with_store(|m| { let p = m.get_mut(payment_id)?; if p.status != "AUTHORIZED" { return None; } p.status = "CAPTURED".into(); Some(p.clone()) })
}
pub fn refund(payment_id: &str) -> Option<Payment> {
    with_store(|m| { let p = m.get_mut(payment_id)?; if p.status != "CAPTURED" { return None; } p.status = "REFUNDED".into(); Some(p.clone()) })
}
