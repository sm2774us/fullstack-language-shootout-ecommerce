// Business logic — hidden from the outside world.
#pragma once
#include <string>
#include <map>
#include <mutex>
#include <optional>

struct Payment { std::string payment_id, order_id, status; long long amount_cents = 0; };

class PaymentStore {
public:
    static PaymentStore& instance() { static PaymentStore s; return s; }
    Payment authorize(const std::string& payment_id, const std::string& order_id, long long amount_cents) {
        std::lock_guard<std::mutex> lock(mu_);
        Payment p{payment_id, order_id, "AUTHORIZED", amount_cents};
        payments_[payment_id] = p;
        return p;
    }
    std::optional<Payment> capture(const std::string& payment_id) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = payments_.find(payment_id);
        if (it == payments_.end() || it->second.status != "AUTHORIZED") return std::nullopt;
        it->second.status = "CAPTURED";
        return it->second;
    }
    std::optional<Payment> refund(const std::string& payment_id) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = payments_.find(payment_id);
        if (it == payments_.end() || it->second.status != "CAPTURED") return std::nullopt;
        it->second.status = "REFUNDED";
        return it->second;
    }
private:
    std::mutex mu_;
    std::map<std::string, Payment> payments_;
};
