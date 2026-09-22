// Real Stripe webhook endpoint for the frontend deployment (used when
// Stripe is configured to call the TanStack Start server directly rather
// than one of the backends). Verifies the signature via the official
// Stripe SDK's `constructEvent`, which implements the same
// HMAC-SHA256 + timestamp-tolerance algorithm documented in
// docs/stripe-webhooks.md and re-implemented natively in every backend.
import { createServerFileRoute } from "@tanstack/react-start/server";
import Stripe from "stripe";

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY ?? "", {
  apiVersion: "2024-11-20.acacia",
});

export const ServerRoute = createServerFileRoute("/webhooks/stripe").methods({
  POST: async ({ request }) => {
    const payload = await request.text();
    const sig = request.headers.get("stripe-signature") ?? "";
    const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET ?? "";

    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(payload, sig, webhookSecret);
    } catch (err) {
      return new Response(
        JSON.stringify({ error: err instanceof Error ? err.message : "invalid signature" }),
        { status: 400, headers: { "Content-Type": "application/json" } },
      );
    }

    switch (event.type) {
      case "payment_intent.succeeded":
        // In production: mark the matching order PAID via the orders
        // module's public-api (see src/server/orders.ts) and enqueue a
        // notifications.send_order_confirmation call.
        break;
      case "payment_intent.payment_failed":
        // In production: mark the order FAILED and notify the customer.
        break;
      default:
        break;
    }

    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  },
});
