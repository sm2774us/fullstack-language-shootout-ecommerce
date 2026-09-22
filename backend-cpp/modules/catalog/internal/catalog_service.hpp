// Business logic — hidden from the outside world.
#pragma once
#include "../infrastructure/seed_data.hpp"
#include <algorithm>
#include <optional>

class CatalogService {
public:
    CatalogService() : products_(seed_products()) {}

    std::vector<Product> list(const std::string& category) const {
        std::vector<Product> out;
        for (auto& p : products_) if (category.empty() || p.category == category) out.push_back(p);
        return out;
    }

    std::optional<Product> get(const std::string& sku) const {
        for (auto& p : products_) if (p.sku == sku) return p;
        return std::nullopt;
    }

    std::vector<Product> search(const std::string& query) const {
        std::string q = query;
        std::transform(q.begin(), q.end(), q.begin(), ::tolower);
        std::vector<Product> out;
        for (auto& p : products_) {
            std::string name = p.name; std::transform(name.begin(), name.end(), name.begin(), ::tolower);
            if (name.find(q) != std::string::npos) out.push_back(p);
        }
        return out;
    }
private:
    std::vector<Product> products_;
};
