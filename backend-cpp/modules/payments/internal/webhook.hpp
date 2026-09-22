// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance) using OpenSSL — mirrors backend-python's webhook_handler.py.
#pragma once
#include <openssl/hmac.h>
#include <string>
#include <sstream>
#include <vector>
#include <ctime>
#include <cstdlib>

inline std::string hmac_sha256_hex(const std::string& key, const std::string& data) {
    unsigned char* digest = HMAC(EVP_sha256(), key.c_str(), (int)key.size(),
        (unsigned char*)data.c_str(), data.size(), nullptr, nullptr);
    std::ostringstream oss;
    for (int i = 0; i < 32; i++) oss << std::hex << (digest[i] >> 4) << (digest[i] & 0xF);
    return oss.str();
}

inline bool verify_stripe_webhook(const std::string& payload, const std::string& sig_header, std::string& err) {
    const char* secret_env = std::getenv("STRIPE_WEBHOOK_SECRET");
    std::string secret = secret_env ? secret_env : "";
    if (secret.empty()) { err = "STRIPE_WEBHOOK_SECRET is not configured"; return false; }

    long timestamp = 0;
    std::vector<std::string> sigs;
    std::istringstream ss(sig_header);
    std::string part;
    while (std::getline(ss, part, ',')) {
        auto eq = part.find('=');
        if (eq == std::string::npos) continue;
        std::string k = part.substr(0, eq), v = part.substr(eq + 1);
        if (k == "t") timestamp = std::atol(v.c_str());
        else if (k == "v1") sigs.push_back(v);
    }
    if (timestamp == 0 || sigs.empty()) { err = "malformed Stripe-Signature header"; return false; }
    long now = (long)std::time(nullptr);
    if (std::labs(now - timestamp) > 300) { err = "timestamp outside tolerance — possible replay"; return false; }

    std::string signed_payload = std::to_string(timestamp) + "." + payload;
    std::string expected = hmac_sha256_hex(secret, signed_payload);
    for (auto& s : sigs) if (s == expected) return true;
    err = "signature mismatch";
    return false;
}
