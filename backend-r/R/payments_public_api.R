#' ONLY interface other modules/apps may import from the payments module.
#' @export
payments_authorize <- function(payment_id, order_id, amount_cents) authorize_payment(payment_id, order_id, amount_cents)
#' @export
payments_capture <- function(payment_id) capture_payment(payment_id)
#' @export
payments_refund <- function(payment_id) refund_payment(payment_id)
