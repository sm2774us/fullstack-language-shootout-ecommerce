import uuid

class NotifyService:
    def send(self, order_id: str, customer_id: str, channel: str) -> dict:
        return {"notification_id": str(uuid.uuid4()), "status": "QUEUED"}
