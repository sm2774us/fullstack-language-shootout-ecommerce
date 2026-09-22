#' ONLY interface other modules/apps may import from the catalog module.
#' @export
catalog_api_list <- function(category = NULL) catalog_list(category)
#' @export
catalog_api_get <- function(sku) catalog_get(sku)
#' @export
catalog_api_search <- function(query) catalog_search(query)
