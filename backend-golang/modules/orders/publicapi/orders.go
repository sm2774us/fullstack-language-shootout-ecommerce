// Package publicapi is the ONLY orders surface other modules/apps may import.
package publicapi

import "github.com/example/ecommerce-shootout/backend-golang/modules/orders/internal"

type Order = internal.Order

func CreateOrder(orderID string, total int64) Order       { return internal.CreateOrder(orderID, total) }
func GetOrder(orderID string) (Order, bool)                { return internal.GetOrder(orderID) }
func MarkPaid(orderID, paymentID string) (Order, bool)      { return internal.MarkPaid(orderID, paymentID) }
func MarkFailed(orderID, reason string) (Order, bool)       { return internal.MarkFailed(orderID, reason) }
func MarkRefunded(orderID string, amountCents int64) (Order, bool) {
	return internal.MarkRefunded(orderID, amountCents)
}
