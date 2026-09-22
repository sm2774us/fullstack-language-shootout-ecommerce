// Business logic — hidden from the outside world.
using BackendCsharp.Modules.Catalog.Infrastructure;
namespace BackendCsharp.Modules.Catalog.Internal;

public class CatalogService
{
    private readonly List<Product> _products = SeedData.Seed();

    public IEnumerable<Product> List(string? category) =>
        _products.Where(p => string.IsNullOrEmpty(category) || p.Category == category);

    public Product? Get(string sku) => _products.FirstOrDefault(p => p.Sku == sku);

    public IEnumerable<Product> Search(string query)
    {
        var q = query.ToLowerInvariant();
        return _products.Where(p => p.Name.ToLowerInvariant().Contains(q) || p.Description.ToLowerInvariant().Contains(q));
    }
}
