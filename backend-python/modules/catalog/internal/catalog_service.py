"""Business logic — hidden from the outside world."""
from modules.catalog.infrastructure.seed_data import SEED_PRODUCTS


class CatalogService:
    def __init__(self):
        self._products = {p["sku"]: p for p in SEED_PRODUCTS}

    def list_products(self, category: str | None = None):
        items = list(self._products.values())
        if category:
            items = [p for p in items if p["category"] == category]
        return items

    def get_product(self, sku: str):
        return self._products.get(sku)

    def search_products(self, query: str):
        q = query.lower()
        return [p for p in self._products.values()
                if q in p["name"].lower() or q in p["description"].lower()]
