//! Business logic — hidden from the outside world.
use super::infrastructure::{seed_products, Product};

pub struct CatalogService { products: Vec<Product> }

impl CatalogService {
    pub fn new() -> Self { Self { products: seed_products() } }

    pub fn list(&self, category: Option<&str>) -> Vec<&Product> {
        self.products.iter()
            .filter(|p| category.map_or(true, |c| p.category == c))
            .collect()
    }

    pub fn get(&self, sku: &str) -> Option<&Product> {
        self.products.iter().find(|p| p.sku == sku)
    }

    pub fn search(&self, query: &str) -> Vec<&Product> {
        let q = query.to_lowercase();
        self.products.iter()
            .filter(|p| p.name.to_lowercase().contains(&q) || p.description.to_lowercase().contains(&q))
            .collect()
    }
}
