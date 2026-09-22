# Generates R message/service bindings from ../proto/ecommerce.proto using
# RProtoBuf at build/load time (R has no stable static-codegen story the
# way protoc plugins do for compiled languages, so this reads the .proto
# directly). Called by `nx run backend-r:generate` and from zzz.R on
# package load.
generate_stubs <- function() {
  if (!requireNamespace("RProtoBuf", quietly = TRUE)) {
    stop("RProtoBuf not installed — run: R -e \"install.packages('RProtoBuf')\"")
  }
  proto_path <- file.path("..", "proto", "ecommerce.proto")
  if (!file.exists(proto_path)) stop(paste("proto file not found:", proto_path))
  RProtoBuf::readProtoFiles(proto_path)
  message("Loaded ecommerce.v1 descriptors from ", proto_path)
  invisible(TRUE)
}
