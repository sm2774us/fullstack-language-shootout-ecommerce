namespace BackendCsharp.Modules.Catalog.Infrastructure;

public record Product(string Sku, string Name, string Description, long PriceCents, string Category, int StockQuantity);

public static class SeedData
{
    public static List<Product> Seed() => new() {
        new("SKU-1001", "Wireless Mechanical Keyboard", "Hot-swappable switches, USB-C.", 8999, "electronics", 42),
        new("SKU-1002", "27in 4K Monitor", "IPS panel, 144Hz.", 39999, "electronics", 15),
        new("SKU-1003", "Ceramic Pour-Over Kettle", "1.2L, gooseneck spout.", 4599, "home", 88),
        new("SKU-1004", "Trail Running Shoes", "Lightweight, grippy outsole.", 12999, "apparel", 60),
        new("SKU-1005", "Stainless Steel Water Bottle", "32oz, insulated.", 2999, "home", 120),
    };
}
