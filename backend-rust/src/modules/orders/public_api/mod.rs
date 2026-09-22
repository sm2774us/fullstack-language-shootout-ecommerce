//! ONLY interface other modules/apps may import from the orders module.
use super::internal;
pub use internal::Order;

pub fn create_order(total_cents: i64) -> Order { internal::create_order(total_cents) }
pub fn get_order(order_id: &str) -> Option<Order> { internal::get_order(order_id) }
pub fn mark_paid(order_id: &str, payment_id: &str) -> Option<Order> { internal::mark_paid(order_id, payment_id) }
pub fn mark_failed(order_id: &str, reason: &str) -> Option<Order> { internal::mark_failed(order_id, reason) }
pub fn mark_refunded(order_id: &str, amount_cents: i64) -> Option<Order> { internal::mark_refunded(order_id, amount_cents) }
