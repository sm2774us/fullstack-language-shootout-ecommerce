// ONLY interface other modules/apps may import from the payments module.
package com.example.modules.payments.publicapi;

import com.example.modules.payments.internal.PaymentService;
import java.util.Optional;

public class PaymentsPublicApi {
    private final PaymentService svc = new PaymentService();
    public PaymentService.Payment authorize(String paymentId, String orderId, long amountCents) { return svc.authorize(paymentId, orderId, amountCents); }
    public Optional<PaymentService.Payment> capture(String paymentId) { return svc.capture(paymentId); }
    public Optional<PaymentService.Payment> refund(String paymentId) { return svc.refund(paymentId); }
}
