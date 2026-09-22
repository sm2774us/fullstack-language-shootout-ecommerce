// Business logic — hidden from the outside world.
package com.example.modules.catalog.internal

import com.example.modules.catalog.infrastructure.{Product, SeedData}

class CatalogService:
  private val products = SeedData.seed

  def list(category: Option[String]): List[Product] =
    products.filter(p => category.forall(c => c.isEmpty || p.category == c))

  def get(sku: String): Option[Product] = products.find(_.sku == sku)

  def search(query: String): List[Product] =
    val q = query.toLowerCase
    products.filter(p => p.name.toLowerCase.contains(q) || p.description.toLowerCase.contains(q))
