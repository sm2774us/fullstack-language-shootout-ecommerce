# Business logic — hidden from the outside world.
module Payments
  module Internal
    class PaymentService
      @@store = {}

      def authorize(payment_id, order_id, amount_cents)
        p = { payment_id: payment_id, order_id: order_id, status: "AUTHORIZED", amount_cents: amount_cents }
        @@store[payment_id] = p
        p
      end

      def capture(payment_id)
        p = @@store[payment_id]
        return nil if p.nil? || p[:status] != "AUTHORIZED"
        p[:status] = "CAPTURED"
        p
      end

      def refund(payment_id)
        p = @@store[payment_id]
        return nil if p.nil? || p[:status] != "CAPTURED"
        p[:status] = "REFUNDED"
        p
      end
    end
  end
end
