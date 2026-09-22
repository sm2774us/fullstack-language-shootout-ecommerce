//! ONLY interface other modules/apps may import from the payments module.
use super::internal;
pub use internal::Payment;

pub fn authorize(payment_id: &str, order_id: &str, amount_cents: i64) -> Payment { internal::authorize(payment_id, order_id, amount_cents) }
pub fn capture(payment_id: &str) -> Option<Payment> { internal::capture(payment_id) }
pub fn refund(payment_id: &str) -> Option<Payment> { internal::refund(payment_id) }
