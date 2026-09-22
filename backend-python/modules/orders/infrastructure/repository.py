"""Orders-specific persistence. Swap for Postgres/Drizzle-equivalent in prod."""
class OrderRepository:
    _store: dict = {}

    def save(self, order: dict) -> None:
        self._store[order["order_id"]] = order

    def find(self, order_id: str) -> dict | None:
        return self._store.get(order_id)
