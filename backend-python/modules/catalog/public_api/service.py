"""ONLY interface other modules/apps may import from the catalog module."""
from modules.catalog.internal.catalog_service import CatalogService


class CatalogPublicApi:
    def __init__(self):
        self._svc = CatalogService()

    def list_products(self, category: str | None = None):
        return self._svc.list_products(category)

    def get_product(self, sku: str):
        return self._svc.get_product(sku)

    def search_products(self, query: str):
        return self._svc.search_products(query)
