# Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
# tolerance) — mirrors backend-python's webhook_handler.py.
require "openssl"

module Payments
  module Internal
    class StripeSignatureError < StandardError; end

    module StripeWebhookVerifier
      TOLERANCE_SECONDS = 300

      def self.verify(payload, sig_header, secret = ENV["STRIPE_WEBHOOK_SECRET"])
        raise StripeSignatureError, "STRIPE_WEBHOOK_SECRET is not configured" if secret.nil? || secret.empty?

        timestamp = nil
        sigs = []
        sig_header.split(",").each do |part|
          key, _, value = part.partition("=")
          case key.strip
          when "t" then timestamp = value.to_i
          when "v1" then sigs << value
          end
        end
        raise StripeSignatureError, "malformed Stripe-Signature header" if timestamp.nil? || sigs.empty?

        now = Time.now.to_i
        raise StripeSignatureError, "timestamp outside tolerance — possible replay" if (now - timestamp).abs > TOLERANCE_SECONDS

        signed_payload = "#{timestamp}.#{payload}"
        expected = OpenSSL::HMAC.hexdigest("SHA256", secret, signed_payload)

        raise StripeSignatureError, "signature mismatch" unless sigs.any? { |s| secure_compare(s, expected) }
        true
      end

      def self.secure_compare(a, b)
        return false unless a.bytesize == b.bytesize
        OpenSSL.fixed_length_secure_compare(a, b)
      end
    end
  end
end
