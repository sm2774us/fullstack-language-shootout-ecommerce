// ONLY interface other modules/apps may import from the catalog module.
package com.example.modules.catalog.publicapi

import com.example.modules.catalog.internal.CatalogService
import com.example.modules.catalog.infrastructure.Product

class CatalogPublicApi:
  private val svc = new CatalogService
  def list(category: Option[String]): List[Product] = svc.list(category)
  def get(sku: String): Option[Product] = svc.get(sku)
  def search(query: String): List[Product] = svc.search(query)
