#include "orders_api.hpp"
Order orders_create(long long total_cents) { return create_order(total_cents); }
