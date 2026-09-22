# Business logic — hidden from the outside world.
.order_store <- new.env(parent = emptyenv())

create_order <- function(order_id, total_cents) {
  order <- list(order_id = order_id, total_cents = total_cents, status = "PENDING")
  assign(order_id, order, envir = .order_store)
  order
}

get_order <- function(order_id) {
  if (!exists(order_id, envir = .order_store, inherits = FALSE)) return(NULL)
  get(order_id, envir = .order_store)
}

# Transitions PENDING -> PAID. Called only after Stripe signature verification.
mark_paid <- function(order_id, payment_id) {
  o <- get_order(order_id)
  if (is.null(o)) return(NULL)
  o$status <- "PAID"; o$payment_id <- payment_id
  assign(order_id, o, envir = .order_store)
  o
}

mark_failed <- function(order_id, reason) {
  o <- get_order(order_id)
  if (is.null(o)) return(NULL)
  o$status <- "CANCELLED"; o$cancel_reason <- reason
  assign(order_id, o, envir = .order_store)
  o
}

mark_refunded <- function(order_id, amount_cents) {
  o <- get_order(order_id)
  if (is.null(o) || o$status != "PAID") return(NULL)
  o$status <- "REFUNDED"; o$refunded_cents <- amount_cents
  assign(order_id, o, envir = .order_store)
  o
}
