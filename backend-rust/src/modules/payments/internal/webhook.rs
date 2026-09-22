//! Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
//! tolerance) — mirrors backend-python's webhook_handler.py. See
//! docs/stripe-webhooks.md for the shared cross-language pattern.
use hmac::{Hmac, Mac};
use sha2::Sha256;
use std::time::{SystemTime, UNIX_EPOCH};

const TOLERANCE_SECS: i64 = 300;

pub fn verify_stripe_webhook(payload: &[u8], sig_header: &str, secret: &str) -> Result<(), String> {
    if secret.is_empty() {
        return Err("STRIPE_WEBHOOK_SECRET is not configured".into());
    }
    let mut timestamp: i64 = 0;
    let mut sigs = Vec::new();
    for part in sig_header.split(',') {
        if let Some((k, v)) = part.split_once('=') {
            match k.trim() {
                "t" => timestamp = v.parse().unwrap_or(0),
                "v1" => sigs.push(v.to_string()),
                _ => {}
            }
        }
    }
    if timestamp == 0 || sigs.is_empty() {
        return Err("malformed Stripe-Signature header".into());
    }
    let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs() as i64;
    if (now - timestamp).abs() > TOLERANCE_SECS {
        return Err("timestamp outside tolerance — possible replay".into());
    }
    let signed_payload = format!("{}.{}", timestamp, String::from_utf8_lossy(payload));
    let mut mac = Hmac::<Sha256>::new_from_slice(secret.as_bytes()).unwrap();
    mac.update(signed_payload.as_bytes());
    let expected = hex::encode(mac.finalize().into_bytes());
    if sigs.iter().any(|s| s == &expected) {
        Ok(())
    } else {
        Err("signature mismatch".into())
    }
}
