package infrastructure

type Product struct {
	SKU         string   `json:"sku"`
	Name        string   `json:"name"`
	Description string   `json:"description"`
	PriceCents  int64    `json:"price_cents"`
	Category    string   `json:"category"`
	Stock       int      `json:"stock_quantity"`
	ImageURLs   []string `json:"image_urls"`
}

func SeedProducts() []Product {
	return []Product{
		{"SKU-1001", "Wireless Mechanical Keyboard", "Hot-swappable switches, USB-C.", 8999, "electronics", 42, nil},
		{"SKU-1002", "27in 4K Monitor", "IPS panel, 144Hz.", 39999, "electronics", 15, nil},
		{"SKU-1003", "Ceramic Pour-Over Kettle", "1.2L, gooseneck spout.", 4599, "home", 88, nil},
		{"SKU-1004", "Trail Running Shoes", "Lightweight, grippy outsole.", 12999, "apparel", 60, nil},
		{"SKU-1005", "Stainless Steel Water Bottle", "32oz, insulated.", 2999, "home", 120, nil},
	}
}
