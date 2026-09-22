// Server function proxying to whichever backend's Catalog public-api is
// under test — same BACKEND_TARGET_URL pattern as src/server/orders.ts.
import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";

const listProductsInput = z.object({ category: z.string().optional() });

export const listProducts = createServerFn({ method: "GET" })
  .validator(listProductsInput)
  .handler(async ({ data }) => {
    const targetUrl = process.env.BACKEND_TARGET_URL ?? "http://localhost:8080";
    const url = new URL("/api/products", targetUrl);
    if (data.category) url.searchParams.set("category", data.category);
    const res = await fetch(url);
    return res.json();
  });

export const searchProducts = createServerFn({ method: "GET" })
  .validator(z.object({ q: z.string() }))
  .handler(async ({ data }) => {
    const targetUrl = process.env.BACKEND_TARGET_URL ?? "http://localhost:8080";
    const url = new URL("/api/products/search", targetUrl);
    url.searchParams.set("q", data.q);
    const res = await fetch(url);
    return res.json();
  });
