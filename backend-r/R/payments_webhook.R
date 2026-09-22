# Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
# tolerance) — mirrors backend-python's webhook_handler.py.
verify_stripe_webhook <- function(payload, sig_header, secret = Sys.getenv("STRIPE_WEBHOOK_SECRET"),
                                   tolerance_seconds = 300) {
  if (!nzchar(secret)) stop("STRIPE_WEBHOOK_SECRET is not configured")

  parts <- strsplit(sig_header, ",")[[1]]
  kv <- lapply(parts, function(p) strsplit(p, "=", fixed = TRUE)[[1]])
  timestamp <- NULL
  sigs <- c()
  for (pair in kv) {
    if (length(pair) != 2) next
    key <- trimws(pair[1]); val <- trimws(pair[2])
    if (key == "t") timestamp <- as.numeric(val)
    if (key == "v1") sigs <- c(sigs, val)
  }
  if (is.null(timestamp) || length(sigs) == 0) stop("malformed Stripe-Signature header")

  now <- as.numeric(Sys.time())
  if (abs(now - timestamp) > tolerance_seconds) stop("timestamp outside tolerance — possible replay")

  signed_payload <- paste0(as.integer(timestamp), ".", payload)
  expected <- digest::hmac(key = secret, object = signed_payload, algo = "sha256", serialize = FALSE)

  if (!(expected %in% sigs)) stop("signature mismatch")
  TRUE
}
