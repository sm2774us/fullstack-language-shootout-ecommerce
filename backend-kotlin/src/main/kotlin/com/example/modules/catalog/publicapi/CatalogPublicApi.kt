// ONLY interface other modules/apps may import from the catalog module.
package com.example.modules.catalog.publicapi

import com.example.modules.catalog.internal.CatalogService

class CatalogPublicApi {
    private val svc = CatalogService()
    fun list(category: String?) = svc.list(category)
    fun get(sku: String) = svc.get(sku)
    fun search(query: String) = svc.search(query)
}
