# Business logic — hidden from the outside world.
.payment_store <- new.env(parent = emptyenv())

authorize_payment <- function(payment_id, order_id, amount_cents) {
  p <- list(payment_id = payment_id, order_id = order_id, status = "AUTHORIZED", amount_cents = amount_cents)
  assign(payment_id, p, envir = .payment_store)
  p
}

capture_payment <- function(payment_id) {
  if (!exists(payment_id, envir = .payment_store, inherits = FALSE)) return(NULL)
  p <- get(payment_id, envir = .payment_store)
  if (p$status != "AUTHORIZED") return(NULL)
  p$status <- "CAPTURED"
  assign(payment_id, p, envir = .payment_store)
  p
}

refund_payment <- function(payment_id) {
  if (!exists(payment_id, envir = .payment_store, inherits = FALSE)) return(NULL)
  p <- get(payment_id, envir = .payment_store)
  if (p$status != "CAPTURED") return(NULL)
  p$status <- "REFUNDED"
  assign(payment_id, p, envir = .payment_store)
  p
}
