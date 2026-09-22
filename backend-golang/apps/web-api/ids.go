package main

import (
	"crypto/rand"
	"fmt"
)

// generateOrderID / generatePaymentID produce simple random hex IDs without
// pulling in an external UUID dependency — swap for github.com/google/uuid
// if the rest of the codebase standardizes on it.
func randomHexID(prefix string) string {
	b := make([]byte, 8)
	_, _ = rand.Read(b)
	return fmt.Sprintf("%s_%x", prefix, b)
}

func generateOrderID() string   { return randomHexID("ord") }
func generatePaymentID() string { return randomHexID("pay") }
