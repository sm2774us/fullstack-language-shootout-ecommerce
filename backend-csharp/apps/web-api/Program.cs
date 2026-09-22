// Single deployable entry point for the C# / .NET backend.
using BackendCsharp.Modules.Orders.Internal;
using BackendCsharp.Modules.Orders.PublicApi;
using BackendCsharp.Modules.Payments.PublicApi;
using BackendCsharp.Modules.Catalog.PublicApi;
using BackendCsharp.Modules.Payments.Internal;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls("http://0.0.0.0:8080");
var app = builder.Build();
var catalog = new CatalogPublicApi();
var orders = new OrdersPublicApi();
var payments = new PaymentsPublicApi();

app.MapGet("/api/perf", () => Results.Json(new
{
    backend = "backend-csharp",
    language = "csharp",
    server_time_unix_ms = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
    status = "ok"
}));

app.MapGet("/healthz", () => Results.Json(new { status = "ok" }));

app.MapGet("/api/products", (string? category) => Results.Json(catalog.List(category)));
app.MapGet("/api/products/search", (string q) => Results.Json(catalog.Search(q)));
app.MapGet("/api/products/{sku}", (string sku) =>
{
    var p = catalog.Get(sku);
    return p is null ? Results.Json(new { error = "not found" }) : Results.Json(p);
});

app.MapPost("/webhooks/stripe", async (HttpRequest req) =>
{
    using var reader = new StreamReader(req.Body);
    var payload = await reader.ReadToEndAsync();
    var sig = req.Headers["Stripe-Signature"].ToString();
    var (ok, error) = StripeWebhookVerifier.Verify(payload, sig, Environment.GetEnvironmentVariable("STRIPE_WEBHOOK_SECRET"));
    if (!ok) return Results.Json(new { error });

    using var doc = JsonDocument.Parse(payload);
    var root = doc.RootElement;
    var eventType = root.TryGetProperty("type", out var t) ? t.GetString() ?? "" : "";
    var dataObj = root.TryGetProperty("data", out var d) && d.TryGetProperty("object", out var o) ? o : default;
    var orderId = dataObj.ValueKind == JsonValueKind.Object && dataObj.TryGetProperty("metadata", out var meta) && meta.TryGetProperty("order_id", out var oid) ? oid.GetString() ?? "" : "";
    var paymentIntentId = dataObj.ValueKind == JsonValueKind.Object && dataObj.TryGetProperty("id", out var pid) ? pid.GetString() ?? "" : "";

    switch (eventType)
    {
        case "payment_intent.succeeded" when orderId != "":
            payments.Capture(paymentIntentId);
            orders.MarkPaid(orderId, paymentIntentId);
            break;
        case "payment_intent.payment_failed" when orderId != "":
            var reason = dataObj.TryGetProperty("last_payment_error", out var lpe) && lpe.TryGetProperty("message", out var msg) ? msg.GetString() ?? "payment failed" : "payment failed";
            orders.MarkFailed(orderId, reason);
            break;
        case "charge.refunded" when orderId != "":
            var amount = dataObj.TryGetProperty("amount_refunded", out var amt) ? amt.GetInt64() : 0;
            orders.MarkRefunded(orderId, amount);
            break;
    }

    return Results.Json(new { received = true, event_type = eventType });
});

app.Run();
