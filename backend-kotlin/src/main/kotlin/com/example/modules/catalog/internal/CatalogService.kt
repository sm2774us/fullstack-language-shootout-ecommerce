// Business logic — hidden from the outside world.
package com.example.modules.catalog.internal

import com.example.modules.catalog.infrastructure.Product
import com.example.modules.catalog.infrastructure.seedProducts

class CatalogService {
    private val products = seedProducts()

    fun list(category: String?) = products.filter { category.isNullOrEmpty() || it.category == category }
    fun get(sku: String): Product? = products.find { it.sku == sku }
    fun search(query: String): List<Product> {
        val q = query.lowercase()
        return products.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }
}
