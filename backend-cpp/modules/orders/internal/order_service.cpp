#include "order_service.hpp"
Order create_order(long long total_cents) {
    return Order{"ord_placeholder", total_cents, "PENDING"};
}
