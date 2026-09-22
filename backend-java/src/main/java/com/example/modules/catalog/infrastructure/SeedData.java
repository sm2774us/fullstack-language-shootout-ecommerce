package com.example.modules.catalog.infrastructure;

import java.util.List;

public class SeedData {
    public record Product(String sku, String name, String description, long priceCents, String category, int stockQuantity) {}

    public static List<Product> seed() {
        return List.of(
            new Product("SKU-1001", "Wireless Mechanical Keyboard", "Hot-swappable switches, USB-C.", 8999, "electronics", 42),
            new Product("SKU-1002", "27in 4K Monitor", "IPS panel, 144Hz.", 39999, "electronics", 15),
            new Product("SKU-1003", "Ceramic Pour-Over Kettle", "1.2L, gooseneck spout.", 4599, "home", 88),
            new Product("SKU-1004", "Trail Running Shoes", "Lightweight, grippy outsole.", 12999, "apparel", 60),
            new Product("SKU-1005", "Stainless Steel Water Bottle", "32oz, insulated.", 2999, "home", 120)
        );
    }
}
