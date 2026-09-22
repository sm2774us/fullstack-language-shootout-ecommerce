# Business logic — hidden from the outside world.
module Orders
  module Internal
    class OrderService
      @@store = {}

      def create_order(order_id, total_cents)
        order = { order_id: order_id, total_cents: total_cents, status: "PENDING" }
        @@store[order_id] = order
        order
      end

      def get_order(order_id)
        @@store[order_id]
      end

      # Transitions PENDING -> PAID. Called only after Stripe signature verification.
      def mark_paid(order_id, payment_id)
        o = @@store[order_id]
        return nil if o.nil?
        o[:status] = "PAID"; o[:payment_id] = payment_id
        o
      end

      def mark_failed(order_id, reason)
        o = @@store[order_id]
        return nil if o.nil?
        o[:status] = "CANCELLED"; o[:cancel_reason] = reason
        o
      end

      def mark_refunded(order_id, amount_cents)
        o = @@store[order_id]
        return nil if o.nil? || o[:status] != "PAID"
        o[:status] = "REFUNDED"; o[:refunded_cents] = amount_cents
        o
      end
    end
  end
end
