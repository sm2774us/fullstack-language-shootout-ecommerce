// Package main is the single deployable entry point for the Go backend.
package main

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"time"

	catalogapi "github.com/example/ecommerce-shootout/backend-golang/modules/catalog/publicapi"
	ordersapi "github.com/example/ecommerce-shootout/backend-golang/modules/orders/publicapi"
	paymentsapi "github.com/example/ecommerce-shootout/backend-golang/modules/payments/publicapi"
	paymentsinternal "github.com/example/ecommerce-shootout/backend-golang/modules/payments/internal"
)

var catalog = catalogapi.NewCatalogPublicApi()

func perfHandler(w http.ResponseWriter, r *http.Request) {
	resp := map[string]any{
		"backend":             "backend-golang",
		"language":            "golang",
		"server_time_unix_ms": time.Now().UnixMilli(),
		"status":              "ok",
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

func healthzHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func productsHandler(w http.ResponseWriter, r *http.Request) {
	category := r.URL.Query().Get("category")
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(catalog.List(category))
}

func productHandler(w http.ResponseWriter, r *http.Request) {
	sku := strings.TrimPrefix(r.URL.Path, "/api/products/")
	p, ok := catalog.Get(sku)
	w.Header().Set("Content-Type", "application/json")
	if !ok {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "not found"})
		return
	}
	json.NewEncoder(w).Encode(p)
}

func searchHandler(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query().Get("q")
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(catalog.Search(q))
}

func stripeWebhookHandler(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "cannot read body", http.StatusBadRequest)
		return
	}
	if err := paymentsinternal.VerifyStripeWebhook(body, r.Header.Get("Stripe-Signature")); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	var event struct {
		Type string `json:"type"`
		Data struct {
			Object struct {
				ID              string `json:"id"`
				Metadata        struct{ OrderID string `json:"order_id"` } `json:"metadata"`
				AmountRefunded  int64  `json:"amount_refunded"`
				LastPaymentError struct{ Message string `json:"message"` } `json:"last_payment_error"`
			} `json:"object"`
		} `json:"data"`
	}
	if err := json.Unmarshal(body, &event); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "malformed event JSON"})
		return
	}

	orderID := event.Data.Object.Metadata.OrderID
	paymentIntentID := event.Data.Object.ID

	switch event.Type {
	case "payment_intent.succeeded":
		if orderID != "" {
			paymentsapi.Capture(paymentIntentID)
			ordersapi.MarkPaid(orderID, paymentIntentID)
		}
	case "payment_intent.payment_failed":
		if orderID != "" {
			ordersapi.MarkFailed(orderID, event.Data.Object.LastPaymentError.Message)
		}
	case "charge.refunded":
		if orderID != "" {
			ordersapi.MarkRefunded(orderID, event.Data.Object.AmountRefunded)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"received": true, "event_type": event.Type})
}

func main() {
	go serveGRPC("50051")

	mux := http.NewServeMux()
	mux.HandleFunc("/api/perf", perfHandler)
	mux.HandleFunc("/healthz", healthzHandler)
	mux.HandleFunc("/api/products/search", searchHandler)
	mux.HandleFunc("/api/products/", productHandler)
	mux.HandleFunc("/api/products", productsHandler)
	mux.HandleFunc("/webhooks/stripe", stripeWebhookHandler)
	http.ListenAndServe(":8080", mux)
}
