// Business logic — hidden from the outside world.
#pragma once
#include <string>
#include <map>
#include <mutex>
#include <optional>

struct Order {
    std::string order_id, status, payment_id, cancel_reason;
    long long total_cents = 0, refunded_cents = 0;
};

class OrderStore {
public:
    static OrderStore& instance() { static OrderStore s; return s; }

    Order create(const std::string& id, long long total_cents) {
        std::lock_guard<std::mutex> lock(mu_);
        Order o{id, "PENDING", "", "", total_cents, 0};
        orders_[id] = o;
        return o;
    }
    std::optional<Order> get(const std::string& id) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = orders_.find(id);
        if (it == orders_.end()) return std::nullopt;
        return it->second;
    }
    std::optional<Order> mark_paid(const std::string& id, const std::string& payment_id) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = orders_.find(id);
        if (it == orders_.end()) return std::nullopt;
        it->second.status = "PAID"; it->second.payment_id = payment_id;
        return it->second;
    }
    std::optional<Order> mark_failed(const std::string& id, const std::string& reason) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = orders_.find(id);
        if (it == orders_.end()) return std::nullopt;
        it->second.status = "CANCELLED"; it->second.cancel_reason = reason;
        return it->second;
    }
    std::optional<Order> mark_refunded(const std::string& id, long long amount_cents) {
        std::lock_guard<std::mutex> lock(mu_);
        auto it = orders_.find(id);
        if (it == orders_.end() || it->second.status != "PAID") return std::nullopt;
        it->second.status = "REFUNDED"; it->second.refunded_cents = amount_cents;
        return it->second;
    }
private:
    std::mutex mu_;
    std::map<std::string, Order> orders_;
};

inline Order create_order(const std::string& id, long long total_cents) { return OrderStore::instance().create(id, total_cents); }
