// Package internal holds orders business logic hidden from other modules.
package internal

import (
	"sync"
	"time"
)

type Order struct {
	OrderID       string `json:"order_id"`
	Total         int64  `json:"total_cents"`
	Status        string `json:"status"`
	CreatedAt     int64  `json:"created_at_unix_ms"`
	PaymentID     string `json:"payment_id,omitempty"`
	CancelReason  string `json:"cancel_reason,omitempty"`
	RefundedCents int64  `json:"refunded_cents,omitempty"`
}

var (
	mu     sync.Mutex
	orders = map[string]*Order{}
)

func CreateOrder(orderID string, total int64) Order {
	mu.Lock()
	defer mu.Unlock()
	o := &Order{OrderID: orderID, Total: total, Status: "PENDING", CreatedAt: time.Now().UnixMilli()}
	orders[orderID] = o
	return *o
}

func GetOrder(orderID string) (Order, bool) {
	mu.Lock()
	defer mu.Unlock()
	o, ok := orders[orderID]
	if !ok {
		return Order{}, false
	}
	return *o, true
}

// MarkPaid transitions PENDING -> PAID. Called by the payments webhook
// handler only after Stripe signature verification has succeeded.
func MarkPaid(orderID, paymentID string) (Order, bool) {
	mu.Lock()
	defer mu.Unlock()
	o, ok := orders[orderID]
	if !ok {
		return Order{}, false
	}
	o.Status = "PAID"
	o.PaymentID = paymentID
	return *o, true
}

// MarkFailed transitions PENDING -> CANCELLED.
func MarkFailed(orderID, reason string) (Order, bool) {
	mu.Lock()
	defer mu.Unlock()
	o, ok := orders[orderID]
	if !ok {
		return Order{}, false
	}
	o.Status = "CANCELLED"
	o.CancelReason = reason
	return *o, true
}

// MarkRefunded transitions PAID -> REFUNDED.
func MarkRefunded(orderID string, amountCents int64) (Order, bool) {
	mu.Lock()
	defer mu.Unlock()
	o, ok := orders[orderID]
	if !ok || o.Status != "PAID" {
		return Order{}, false
	}
	o.Status = "REFUNDED"
	o.RefundedCents = amountCents
	return *o, true
}
