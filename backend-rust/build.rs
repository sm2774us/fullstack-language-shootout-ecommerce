// Runs on every `cargo build`: compiles ../../proto/ecommerce.proto into
// Rust gRPC client+server stubs under OUT_DIR, which src/main.rs includes
// via `tonic::include_proto!("ecommerce.v1")`.
fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_build::configure()
        .build_server(true)
        .build_client(false)
        .compile_protos(&["../proto/ecommerce.proto"], &["../proto"])?;
    println!("cargo:rerun-if-changed=../proto/ecommerce.proto");
    Ok(())
}
