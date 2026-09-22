# ONLY interface other modules/apps may import from the catalog module.
require_relative "../internal/catalog_service"

module Catalog
  module PublicApi
    class CatalogPublicApi
      def initialize
        @svc = Catalog::Internal::CatalogService.new
      end
      def list(category = nil) = @svc.list(category)
      def get(sku) = @svc.get(sku)
      def search(query) = @svc.search(query)
    end
  end
end
