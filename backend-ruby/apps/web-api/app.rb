# Single deployable entry point for the Ruby backend.
require "sinatra/base"
require "json"
require_relative "../../modules/catalog/public_api/catalog_public_api"
require_relative "../../modules/orders/public_api/orders_public_api"
require_relative "../../modules/payments/public_api/payments_public_api"
require_relative "../../modules/payments/internal/stripe_webhook_verifier"

class WebApi < Sinatra::Base
  set :catalog, Catalog::PublicApi::CatalogPublicApi.new
  set :orders, Orders::PublicApi::OrdersPublicApi.new
  set :payments, Payments::PublicApi::PaymentsPublicApi.new

  get "/api/perf" do
    content_type :json
    {
      backend: "backend-ruby",
      language: "ruby",
      server_time_unix_ms: (Time.now.to_f * 1000).to_i,
      status: "ok"
    }.to_json
  end

  get "/healthz" do
    content_type :json
    { status: "ok" }.to_json
  end

  get "/api/products" do
    content_type :json
    settings.catalog.list(params["category"]).to_json
  end

  get "/api/products/search" do
    content_type :json
    settings.catalog.search(params["q"]).to_json
  end

  get "/api/products/:sku" do
    content_type :json
    product = settings.catalog.get(params["sku"])
    (product || { error: "not found" }).to_json
  end

  post "/webhooks/stripe" do
    content_type :json
    request.body.rewind
    payload = request.body.read
    sig = request.env["HTTP_STRIPE_SIGNATURE"]
    begin
      Payments::Internal::StripeWebhookVerifier.verify(payload, sig)
      event = JSON.parse(payload)
      event_type = event["type"] || ""
      data_obj = event.dig("data", "object") || {}
      order_id = data_obj.dig("metadata", "order_id") || ""
      payment_intent_id = data_obj["id"] || ""

      case event_type
      when "payment_intent.succeeded"
        unless order_id.empty?
          settings.payments.capture(payment_intent_id)
          settings.orders.mark_paid(order_id, payment_intent_id)
        end
      when "payment_intent.payment_failed"
        unless order_id.empty?
          reason = data_obj.dig("last_payment_error", "message") || "payment failed"
          settings.orders.mark_failed(order_id, reason)
        end
      when "charge.refunded"
        unless order_id.empty?
          amount = data_obj["amount_refunded"] || 0
          settings.orders.mark_refunded(order_id, amount)
        end
      end

      { received: true, event_type: event_type }.to_json
    rescue Payments::Internal::StripeSignatureError => e
      status 400
      { error: e.message }.to_json
    end
  end
end
