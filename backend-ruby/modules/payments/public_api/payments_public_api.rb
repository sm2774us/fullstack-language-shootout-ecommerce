# ONLY interface other modules/apps may import from the payments module.
require_relative "../internal/payment_service"

module Payments
  module PublicApi
    class PaymentsPublicApi
      def initialize
        @svc = Payments::Internal::PaymentService.new
      end

      def authorize(payment_id, order_id, amount_cents) = @svc.authorize(payment_id, order_id, amount_cents)
      def capture(payment_id) = @svc.capture(payment_id)
      def refund(payment_id) = @svc.refund(payment_id)
    end
  end
end
