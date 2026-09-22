# Single deployable entry point for the R backend.
library(plumber)
source("R/catalog_internal.R")
source("R/catalog_public_api.R")
source("R/payments_webhook.R")
source("R/orders_internal.R")
source("R/orders_public_api.R")
source("R/payments_internal.R")
source("R/payments_public_api.R")

#* @get /api/perf
function() {
  list(
    backend = "backend-r",
    language = "r",
    server_time_unix_ms = as.numeric(Sys.time()) * 1000,
    status = "ok"
  )
}

#* @get /healthz
function() list(status = "ok")

#* @get /api/products
#* @param category
function(category = NULL) catalog_api_list(category)

#* @get /api/products/search
#* @param q
function(q) catalog_api_search(q)

#* @get /api/products/<sku>
function(sku) {
  p <- catalog_api_get(sku)
  if (is.null(p)) list(error = "not found") else p
}

#* @post /webhooks/stripe
#* @param req
function(req, res) {
  payload <- req$postBody
  sig <- req$HTTP_STRIPE_SIGNATURE
  tryCatch({
    verify_stripe_webhook(payload, sig)
    event <- jsonlite::fromJSON(payload, simplifyVector = FALSE)
    event_type <- if (!is.null(event$type)) event$type else ""
    data_obj <- if (!is.null(event$data) && !is.null(event$data$`object`)) event$data$`object` else list()
    order_id <- if (!is.null(data_obj$metadata) && !is.null(data_obj$metadata$order_id)) data_obj$metadata$order_id else ""
    payment_intent_id <- if (!is.null(data_obj$id)) data_obj$id else ""

    if (event_type == "payment_intent.succeeded" && nzchar(order_id)) {
      payments_capture(payment_intent_id)
      orders_mark_paid(order_id, payment_intent_id)
    } else if (event_type == "payment_intent.payment_failed" && nzchar(order_id)) {
      reason <- if (!is.null(data_obj$last_payment_error) && !is.null(data_obj$last_payment_error$message)) data_obj$last_payment_error$message else "payment failed"
      orders_mark_failed(order_id, reason)
    } else if (event_type == "charge.refunded" && nzchar(order_id)) {
      amount <- if (!is.null(data_obj$amount_refunded)) data_obj$amount_refunded else 0
      orders_mark_refunded(order_id, amount)
    }

    list(received = TRUE, event_type = event_type)
  }, error = function(e) {
    res$status <- 400
    list(error = conditionMessage(e))
  })
}
