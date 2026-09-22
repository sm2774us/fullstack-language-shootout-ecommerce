.onLoad <- function(libname, pkgname) {
  try(generate_stubs(), silent = TRUE)
}
