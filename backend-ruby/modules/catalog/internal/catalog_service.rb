# Business logic — hidden from the outside world.
require_relative "../infrastructure/seed_data"

module Catalog
  module Internal
    class CatalogService
      def list(category = nil)
        products = Catalog::Infrastructure::SEED_PRODUCTS
        category && !category.empty? ? products.select { |p| p[:category] == category } : products
      end

      def get(sku)
        Catalog::Infrastructure::SEED_PRODUCTS.find { |p| p[:sku] == sku }
      end

      def search(query)
        q = query.downcase
        Catalog::Infrastructure::SEED_PRODUCTS.select do |p|
          p[:name].downcase.include?(q) || p[:description].downcase.include?(q)
        end
      end
    end
  end
end
