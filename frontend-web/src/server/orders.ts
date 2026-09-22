// TanStack Start server function — the frontend's only entry point into the
// orders module's public-api. Wraps a gRPC-Web call to whichever backend
// is selected for the shootout run (BACKEND_TARGET_URL env var).
import { createServerFn } from "@tanstack/react-start";
import { createOrderSchema } from "../lib/schemas";

export const createOrder = createServerFn({ method: "POST" })
  .validator(createOrderSchema)
  .handler(async ({ data }) => {
    const targetUrl = process.env.BACKEND_TARGET_URL ?? "http://localhost:8080";
    const res = await fetch(`${targetUrl}/api/orders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    return res.json();
  });
