// Package publicapi is the ONLY payments surface other modules/apps may import.
package publicapi

import "github.com/example/ecommerce-shootout/backend-golang/modules/payments/internal"

type Payment = internal.Payment

func Authorize(paymentID, orderID string, amountCents int64) Payment {
	return internal.Authorize(paymentID, orderID, amountCents)
}
func Capture(paymentID string) (Payment, bool)                    { return internal.Capture(paymentID) }
func Refund(paymentID string, amountCents int64) (Payment, bool)  { return internal.Refund(paymentID, amountCents) }

// VerifyStripeWebhook is the only way to reach the webhook signature
// verification logic from outside the payments module — apps/web-api must
// not (and, per Go's own internal-package visibility rules, cannot) import
// modules/payments/internal directly.
func VerifyStripeWebhook(payload []byte, sigHeader string) error {
	return internal.VerifyStripeWebhook(payload, sigHeader)
}
