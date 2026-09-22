// Exists solely so `cargo build --bin codegen-check` runs build.rs (which
// invokes tonic-build against proto/ecommerce.proto) without compiling the
// full server. Nx's `generate` target calls this.
fn main() {
    println!("proto stubs generated in OUT_DIR");
}
