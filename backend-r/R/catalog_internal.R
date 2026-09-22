# Business logic — hidden from the outside world.
seed_products <- function() {
  list(
    list(sku="SKU-1001", name="Wireless Mechanical Keyboard", description="Hot-swappable switches, USB-C.", price_cents=8999, category="electronics", stock_quantity=42),
    list(sku="SKU-1002", name="27in 4K Monitor", description="IPS panel, 144Hz.", price_cents=39999, category="electronics", stock_quantity=15),
    list(sku="SKU-1003", name="Ceramic Pour-Over Kettle", description="1.2L, gooseneck spout.", price_cents=4599, category="home", stock_quantity=88),
    list(sku="SKU-1004", name="Trail Running Shoes", description="Lightweight, grippy outsole.", price_cents=12999, category="apparel", stock_quantity=60),
    list(sku="SKU-1005", name="Stainless Steel Water Bottle", description="32oz, insulated.", price_cents=2999, category="home", stock_quantity=120)
  )
}

catalog_list <- function(category = NULL) {
  products <- seed_products()
  if (!is.null(category) && nzchar(category)) {
    products <- Filter(function(p) p$category == category, products)
  }
  products
}

catalog_get <- function(sku) {
  products <- seed_products()
  match <- Filter(function(p) p$sku == sku, products)
  if (length(match) == 0) NULL else match[[1]]
}

catalog_search <- function(query) {
  q <- tolower(query)
  products <- seed_products()
  Filter(function(p) grepl(q, tolower(p$name), fixed = TRUE) || grepl(q, tolower(p$description), fixed = TRUE), products)
}
