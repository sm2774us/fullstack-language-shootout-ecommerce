// Package internal holds payments business logic hidden from other modules.
package internal

import "sync"

type Payment struct {
	PaymentID     string `json:"payment_id"`
	OrderID       string `json:"order_id"`
	Status        string `json:"status"`
	AmountCents   int64  `json:"amount_cents"`
	RefundedCents int64  `json:"refunded_cents,omitempty"`
}

var (
	mu       sync.Mutex
	payments = map[string]*Payment{}
)

func Authorize(paymentID, orderID string, amountCents int64) Payment {
	mu.Lock()
	defer mu.Unlock()
	p := &Payment{PaymentID: paymentID, OrderID: orderID, Status: "AUTHORIZED", AmountCents: amountCents}
	payments[paymentID] = p
	return *p
}

func Capture(paymentID string) (Payment, bool) {
	mu.Lock()
	defer mu.Unlock()
	p, ok := payments[paymentID]
	if !ok || p.Status != "AUTHORIZED" {
		return Payment{}, false
	}
	p.Status = "CAPTURED"
	return *p, true
}

func Refund(paymentID string, amountCents int64) (Payment, bool) {
	mu.Lock()
	defer mu.Unlock()
	p, ok := payments[paymentID]
	if !ok || p.Status != "CAPTURED" {
		return Payment{}, false
	}
	p.Status = "REFUNDED"
	p.RefundedCents = amountCents
	return *p, true
}
