// Single deployable entry point for the C++ backend.
#include <httplib.h>
#include <nlohmann/json.hpp>
#include "../../modules/catalog/public_api/catalog_api.hpp"
#include "../../modules/orders/public_api/orders_api.hpp"
#include "../../modules/payments/internal/webhook.hpp"
#include "../../modules/payments/public_api/service.hpp"
#include <chrono>
using json = nlohmann::json;

json product_to_json(const Product& p) {
    return {{"sku", p.sku}, {"name", p.name}, {"description", p.description},
            {"category", p.category}, {"price_cents", p.price_cents}, {"stock_quantity", p.stock_quantity}};
}

int main() {
    httplib::Server svr;
    static CatalogPublicApi catalog;

    svr.Get("/api/products", [](const httplib::Request& req, httplib::Response& res) {
        std::string category = req.has_param("category") ? req.get_param_value("category") : "";
        json arr = json::array();
        for (auto& p : catalog.list(category)) arr.push_back(product_to_json(p));
        res.set_content(arr.dump(), "application/json");
    });

    svr.Get("/api/products/search", [](const httplib::Request& req, httplib::Response& res) {
        std::string q = req.has_param("q") ? req.get_param_value("q") : "";
        json arr = json::array();
        for (auto& p : catalog.search(q)) arr.push_back(product_to_json(p));
        res.set_content(arr.dump(), "application/json");
    });

    svr.Get(R"(/api/products/([^/]+))", [](const httplib::Request& req, httplib::Response& res) {
        auto p = catalog.get(req.matches[1]);
        if (!p) { res.set_content(R"({"error":"not found"})", "application/json"); return; }
        res.set_content(product_to_json(*p).dump(), "application/json");
    });

    svr.Post("/webhooks/stripe", [](const httplib::Request& req, httplib::Response& res) {
        std::string err;
        std::string sig = req.get_header_value("Stripe-Signature");
        if (!verify_stripe_webhook(req.body, sig, err)) {
            res.set_content(json{{"error", err}}.dump(), "application/json");
            return;
        }
        json event;
        try {
            event = json::parse(req.body);
        } catch (...) {
            res.set_content(R"({"error":"malformed event JSON"})", "application/json");
            return;
        }
        std::string event_type = event.value("type", "");
        auto data_obj = event.value("data", json::object()).value("object", json::object());
        std::string order_id = data_obj.value("metadata", json::object()).value("order_id", "");
        std::string payment_intent_id = data_obj.value("id", "");

        if (event_type == "payment_intent.succeeded" && !order_id.empty()) {
            payments_capture(payment_intent_id);
            orders_mark_paid(order_id, payment_intent_id);
        } else if (event_type == "payment_intent.payment_failed" && !order_id.empty()) {
            std::string reason = data_obj.value("last_payment_error", json::object()).value("message", "payment failed");
            orders_mark_failed(order_id, reason);
        } else if (event_type == "charge.refunded" && !order_id.empty()) {
            long long amount = data_obj.value("amount_refunded", 0LL);
            orders_mark_refunded(order_id, amount);
        }
        res.set_content(json{{"received", true}, {"event_type", event_type}}.dump(), "application/json");
    });

    svr.Get("/api/perf", [](const httplib::Request&, httplib::Response& res) {
        auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        json body = {
            {"backend", "backend-cpp"},
            {"language", "cpp"},
            {"server_time_unix_ms", now},
            {"status", "ok"}
        };
        res.set_content(body.dump(), "application/json");
    });

    svr.Get("/healthz", [](const httplib::Request&, httplib::Response& res) {
        res.set_content(R"({"status":"ok"})", "application/json");
    });

    svr.listen("0.0.0.0", 8080);
}
