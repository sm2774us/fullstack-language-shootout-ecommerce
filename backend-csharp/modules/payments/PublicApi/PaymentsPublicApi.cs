// ONLY interface other modules/apps may import from the payments module.
using BackendCsharp.Modules.Payments.Internal;
namespace BackendCsharp.Modules.Payments.PublicApi;

public class PaymentsPublicApi
{
    private readonly PaymentService _svc = new();
    public Payment Authorize(string paymentId, string orderId, long amountCents) => _svc.Authorize(paymentId, orderId, amountCents);
    public Payment? Capture(string paymentId) => _svc.Capture(paymentId);
    public Payment? Refund(string paymentId) => _svc.Refund(paymentId);
}
