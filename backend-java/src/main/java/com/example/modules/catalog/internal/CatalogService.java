// Business logic — hidden from the outside world.
package com.example.modules.catalog.internal;

import com.example.modules.catalog.infrastructure.SeedData;
import com.example.modules.catalog.infrastructure.SeedData.Product;

import java.util.List;
import java.util.Optional;

public class CatalogService {
    private final List<Product> products = SeedData.seed();

    public List<Product> list(String category) {
        return products.stream()
            .filter(p -> category == null || category.isBlank() || p.category().equals(category))
            .toList();
    }

    public Optional<Product> get(String sku) {
        return products.stream().filter(p -> p.sku().equals(sku)).findFirst();
    }

    public List<Product> search(String query) {
        String q = query.toLowerCase();
        return products.stream()
            .filter(p -> p.name().toLowerCase().contains(q) || p.description().toLowerCase().contains(q))
            .toList();
    }
}
