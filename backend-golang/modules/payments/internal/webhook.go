// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance), following the same algorithm as backend-python's
// webhook_handler.py — see docs/stripe-webhooks.md for the shared pattern.
package internal

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

const defaultToleranceSeconds = 300

func VerifyStripeWebhook(payload []byte, sigHeader string) error {
	secret := os.Getenv("STRIPE_WEBHOOK_SECRET")
	if secret == "" {
		return errors.New("STRIPE_WEBHOOK_SECRET is not configured")
	}

	var timestamp int64
	var sigs []string
	for _, part := range strings.Split(sigHeader, ",") {
		kv := strings.SplitN(part, "=", 2)
		if len(kv) != 2 {
			continue
		}
		switch strings.TrimSpace(kv[0]) {
		case "t":
			timestamp, _ = strconv.ParseInt(kv[1], 10, 64)
		case "v1":
			sigs = append(sigs, kv[1])
		}
	}
	if timestamp == 0 || len(sigs) == 0 {
		return errors.New("malformed Stripe-Signature header")
	}

	now := time.Now().Unix()
	if diff := now - timestamp; diff > defaultToleranceSeconds || diff < -defaultToleranceSeconds {
		return errors.New("timestamp outside tolerance — possible replay")
	}

	signedPayload := fmt.Sprintf("%d.%s", timestamp, payload)
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(signedPayload))
	expected := hex.EncodeToString(mac.Sum(nil))

	for _, sig := range sigs {
		if hmac.Equal([]byte(expected), []byte(sig)) {
			return nil
		}
	}
	return errors.New("signature mismatch")
}
