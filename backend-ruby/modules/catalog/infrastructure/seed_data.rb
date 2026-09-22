module Catalog
  module Infrastructure
    SEED_PRODUCTS = [
      { sku: "SKU-1001", name: "Wireless Mechanical Keyboard", description: "Hot-swappable switches, USB-C.", price_cents: 8999, category: "electronics", stock_quantity: 42 },
      { sku: "SKU-1002", name: "27in 4K Monitor", description: "IPS panel, 144Hz.", price_cents: 39999, category: "electronics", stock_quantity: 15 },
      { sku: "SKU-1003", name: "Ceramic Pour-Over Kettle", description: "1.2L, gooseneck spout.", price_cents: 4599, category: "home", stock_quantity: 88 },
      { sku: "SKU-1004", name: "Trail Running Shoes", description: "Lightweight, grippy outsole.", price_cents: 12999, category: "apparel", stock_quantity: 60 },
      { sku: "SKU-1005", name: "Stainless Steel Water Bottle", description: "32oz, insulated.", price_cents: 2999, category: "home", stock_quantity: 120 },
    ].freeze
  end
end
