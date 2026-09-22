// ONLY interface other modules/apps may import from the orders module.
using BackendCsharp.Modules.Orders.Internal;
namespace BackendCsharp.Modules.Orders.PublicApi;

public class OrdersPublicApi
{
    private readonly OrderService _svc = new();
    public Order CreateOrder(string orderId, long totalCents) => _svc.CreateOrder(orderId, totalCents);
    public Order? GetOrder(string orderId) => _svc.GetOrder(orderId);
    public Order? MarkPaid(string orderId, string paymentId) => _svc.MarkPaid(orderId, paymentId);
    public Order? MarkFailed(string orderId, string reason) => _svc.MarkFailed(orderId, reason);
    public Order? MarkRefunded(string orderId, long amountCents) => _svc.MarkRefunded(orderId, amountCents);
}
