// ONLY interface other modules/apps may import from the catalog module.
package com.example.modules.catalog.publicapi;

import com.example.modules.catalog.internal.CatalogService;
import com.example.modules.catalog.infrastructure.SeedData.Product;

import java.util.List;
import java.util.Optional;

public class CatalogPublicApi {
    private final CatalogService svc = new CatalogService();
    public List<Product> list(String category) { return svc.list(category); }
    public Optional<Product> get(String sku) { return svc.get(sku); }
    public List<Product> search(String query) { return svc.search(query); }
}
