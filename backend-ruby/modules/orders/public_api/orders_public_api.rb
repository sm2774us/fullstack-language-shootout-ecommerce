# ONLY interface other modules/apps may import from the orders module.
require_relative "../internal/order_service"

module Orders
  module PublicApi
    class OrdersPublicApi
      def initialize
        @svc = Orders::Internal::OrderService.new
      end

      def create_order(order_id, total_cents) = @svc.create_order(order_id, total_cents)
      def get_order(order_id) = @svc.get_order(order_id)
      def mark_paid(order_id, payment_id) = @svc.mark_paid(order_id, payment_id)
      def mark_failed(order_id, reason) = @svc.mark_failed(order_id, reason)
      def mark_refunded(order_id, amount_cents) = @svc.mark_refunded(order_id, amount_cents)
    end
  end
end
