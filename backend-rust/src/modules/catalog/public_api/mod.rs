//! ONLY interface other modules/apps may import from the catalog module.
use super::internal::CatalogService;
pub use super::infrastructure::Product;

pub struct CatalogPublicApi { svc: CatalogService }

impl CatalogPublicApi {
    pub fn new() -> Self { Self { svc: CatalogService::new() } }
    pub fn list(&self, category: Option<&str>) -> Vec<&Product> { self.svc.list(category) }
    pub fn get(&self, sku: &str) -> Option<&Product> { self.svc.get(sku) }
    pub fn search(&self, query: &str) -> Vec<&Product> { self.svc.search(query) }
}
