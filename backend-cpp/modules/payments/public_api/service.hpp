// ONLY interface other modules/apps may import from the payments module.
#pragma once
#include "../internal/service.hpp"

inline Payment payments_authorize(const std::string& payment_id, const std::string& order_id, long long amount_cents) {
    return PaymentStore::instance().authorize(payment_id, order_id, amount_cents);
}
inline std::optional<Payment> payments_capture(const std::string& payment_id) {
    return PaymentStore::instance().capture(payment_id);
}
inline std::optional<Payment> payments_refund(const std::string& payment_id) {
    return PaymentStore::instance().refund(payment_id);
}
