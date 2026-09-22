// Business logic — hidden from the outside world.
using System.Collections.Concurrent;
namespace BackendCsharp.Modules.Payments.Internal;

public class Payment
{
    public string PaymentId { get; set; } = "";
    public string OrderId { get; set; } = "";
    public string Status { get; set; } = "";
    public long AmountCents { get; set; }
}

public class PaymentService
{
    private static readonly ConcurrentDictionary<string, Payment> Store = new();

    public Payment Authorize(string paymentId, string orderId, long amountCents)
    {
        var p = new Payment { PaymentId = paymentId, OrderId = orderId, Status = "AUTHORIZED", AmountCents = amountCents };
        Store[paymentId] = p;
        return p;
    }

    public Payment? Capture(string paymentId)
    {
        if (!Store.TryGetValue(paymentId, out var p) || p.Status != "AUTHORIZED") return null;
        p.Status = "CAPTURED";
        return p;
    }

    public Payment? Refund(string paymentId)
    {
        if (!Store.TryGetValue(paymentId, out var p) || p.Status != "CAPTURED") return null;
        p.Status = "REFUNDED";
        return p;
    }
}
