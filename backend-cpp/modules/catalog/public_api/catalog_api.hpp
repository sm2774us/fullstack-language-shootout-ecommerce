// ONLY interface other modules/apps may import from the catalog module.
#pragma once
#include "../internal/catalog_service.hpp"

class CatalogPublicApi {
public:
    std::vector<Product> list(const std::string& category = "") { return svc_.list(category); }
    std::optional<Product> get(const std::string& sku) { return svc_.get(sku); }
    std::vector<Product> search(const std::string& query) { return svc_.search(query); }
private:
    CatalogService svc_;
};
