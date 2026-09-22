// ONLY interface other modules/apps may import from the catalog module.
using BackendCsharp.Modules.Catalog.Internal;
using BackendCsharp.Modules.Catalog.Infrastructure;
namespace BackendCsharp.Modules.Catalog.PublicApi;

public class CatalogPublicApi
{
    private readonly CatalogService _svc = new();
    public IEnumerable<Product> List(string? category) => _svc.List(category);
    public Product? Get(string sku) => _svc.Get(sku);
    public IEnumerable<Product> Search(string query) => _svc.Search(query);
}
