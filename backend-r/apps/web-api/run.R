# Boot script used by the Dockerfile / devtools workflow.
pr <- plumber::plumb("apps/web-api/plumber.R")
pr$run(host = "0.0.0.0", port = 8080)
