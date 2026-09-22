// Business logic — hidden from the outside world.
using System.Collections.Concurrent;
namespace BackendCsharp.Modules.Orders.Internal;

public class Order
{
    public string OrderId { get; set; } = "";
    public long TotalCents { get; set; }
    public string Status { get; set; } = "PENDING";
    public string? PaymentId { get; set; }
    public string? CancelReason { get; set; }
    public long? RefundedCents { get; set; }
}

public class OrderService
{
    private static readonly ConcurrentDictionary<string, Order> Store = new();

    public Order CreateOrder(string orderId, long totalCents)
    {
        var o = new Order { OrderId = orderId, TotalCents = totalCents, Status = "PENDING" };
        Store[orderId] = o;
        return o;
    }

    public Order? GetOrder(string orderId) => Store.GetValueOrDefault(orderId);

    public Order? MarkPaid(string orderId, string paymentId)
    {
        if (!Store.TryGetValue(orderId, out var o)) return null;
        o.Status = "PAID"; o.PaymentId = paymentId;
        return o;
    }

    public Order? MarkFailed(string orderId, string reason)
    {
        if (!Store.TryGetValue(orderId, out var o)) return null;
        o.Status = "CANCELLED"; o.CancelReason = reason;
        return o;
    }

    public Order? MarkRefunded(string orderId, long amountCents)
    {
        if (!Store.TryGetValue(orderId, out var o) || o.Status != "PAID") return null;
        o.Status = "REFUNDED"; o.RefundedCents = amountCents;
        return o;
    }
}
