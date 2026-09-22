package com.example.modules.catalog.infrastructure

final case class Product(sku: String, name: String, description: String,
                          priceCents: Long, category: String, stockQuantity: Int)

object SeedData:
  def seed: List[Product] = List(
    Product("SKU-1001", "Wireless Mechanical Keyboard", "Hot-swappable switches, USB-C.", 8999, "electronics", 42),
    Product("SKU-1002", "27in 4K Monitor", "IPS panel, 144Hz.", 39999, "electronics", 15),
    Product("SKU-1003", "Ceramic Pour-Over Kettle", "1.2L, gooseneck spout.", 4599, "home", 88),
    Product("SKU-1004", "Trail Running Shoes", "Lightweight, grippy outsole.", 12999, "apparel", 60),
    Product("SKU-1005", "Stainless Steel Water Bottle", "32oz, insulated.", 2999, "home", 120),
  )
