// ONLY interface other modules/apps may import from the orders module.
#pragma once
#include "../internal/order_service.hpp"
inline Order orders_create(const std::string& id, long long total_cents) { return OrderStore::instance().create(id, total_cents); }
inline std::optional<Order> orders_get(const std::string& id) { return OrderStore::instance().get(id); }
inline std::optional<Order> orders_mark_paid(const std::string& id, const std::string& payment_id) { return OrderStore::instance().mark_paid(id, payment_id); }
inline std::optional<Order> orders_mark_failed(const std::string& id, const std::string& reason) { return OrderStore::instance().mark_failed(id, reason); }
inline std::optional<Order> orders_mark_refunded(const std::string& id, long long amount_cents) { return OrderStore::instance().mark_refunded(id, amount_cents); }
