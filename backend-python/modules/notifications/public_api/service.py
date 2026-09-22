"""ONLY interface other modules/apps may import from the notifications module."""
from modules.notifications.internal.notify_service import NotifyService

class NotificationsPublicApi:
    def __init__(self):
        self._svc = NotifyService()

    def send_order_confirmation(self, order_id: str, customer_id: str, channel: str) -> dict:
        return self._svc.send(order_id, customer_id, channel)
