#' ONLY interface other modules/apps may import from the orders module.
#' @export
orders_create <- function(order_id, total_cents) create_order(order_id, total_cents)
#' @export
orders_get <- function(order_id) get_order(order_id)
#' @export
orders_mark_paid <- function(order_id, payment_id) mark_paid(order_id, payment_id)
#' @export
orders_mark_failed <- function(order_id, reason) mark_failed(order_id, reason)
#' @export
orders_mark_refunded <- function(order_id, amount_cents) mark_refunded(order_id, amount_cents)
