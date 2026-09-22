// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance) — mirrors backend-python's webhook_handler.py.
using System.Security.Cryptography;
using System.Text;
namespace BackendCsharp.Modules.Payments.Internal;

public static class StripeWebhookVerifier
{
    private const long ToleranceSeconds = 300;

    public static (bool ok, string? error) Verify(string payload, string sigHeader, string? secret)
    {
        if (string.IsNullOrEmpty(secret)) return (false, "STRIPE_WEBHOOK_SECRET is not configured");

        long timestamp = 0;
        var sigs = new List<string>();
        foreach (var part in sigHeader.Split(','))
        {
            var kv = part.Split('=', 2);
            if (kv.Length != 2) continue;
            if (kv[0].Trim() == "t") long.TryParse(kv[1], out timestamp);
            else if (kv[0].Trim() == "v1") sigs.Add(kv[1]);
        }
        if (timestamp == 0 || sigs.Count == 0) return (false, "malformed Stripe-Signature header");

        var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        if (Math.Abs(now - timestamp) > ToleranceSeconds) return (false, "timestamp outside tolerance");

        var signedPayload = $"{timestamp}.{payload}";
        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret));
        var expected = Convert.ToHexStringLower(hmac.ComputeHash(Encoding.UTF8.GetBytes(signedPayload)));

        return sigs.Any(s => CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(s), Encoding.UTF8.GetBytes(expected)))
            ? (true, null) : (false, "signature mismatch");
    }
}
