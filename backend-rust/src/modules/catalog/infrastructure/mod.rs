//! Seed product data — swap for a real Postgres-backed repository in prod.
use serde::Serialize;

#[derive(Clone, Serialize)]
pub struct Product {
    pub sku: String,
    pub name: String,
    pub description: String,
    pub price_cents: i64,
    pub category: String,
    pub stock_quantity: i32,
}

pub fn seed_products() -> Vec<Product> {
    vec![
        Product { sku: "SKU-1001".into(), name: "Wireless Mechanical Keyboard".into(), description: "Hot-swappable switches, USB-C.".into(), price_cents: 8999, category: "electronics".into(), stock_quantity: 42 },
        Product { sku: "SKU-1002".into(), name: "27in 4K Monitor".into(), description: "IPS panel, 144Hz.".into(), price_cents: 39999, category: "electronics".into(), stock_quantity: 15 },
        Product { sku: "SKU-1003".into(), name: "Ceramic Pour-Over Kettle".into(), description: "1.2L, gooseneck spout.".into(), price_cents: 4599, category: "home".into(), stock_quantity: 88 },
        Product { sku: "SKU-1004".into(), name: "Trail Running Shoes".into(), description: "Lightweight, grippy outsole.".into(), price_cents: 12999, category: "apparel".into(), stock_quantity: 60 },
        Product { sku: "SKU-1005".into(), name: "Stainless Steel Water Bottle".into(), description: "32oz, insulated.".into(), price_cents: 2999, category: "home".into(), stock_quantity: 120 },
    ]
}
