// Storefront home route — SSR'd product grid backed by TanStack Query.
import { createFileRoute } from "@tanstack/react-router";
import { useCartStore } from "../store/cart";

export const Route = createFileRoute("/")({
  component: Home,
});

function Home() {
  const add = useCartStore((s) => s.add);
  return (
    <main className="mx-auto max-w-6xl p-6">
      <h1 className="text-2xl font-semibold">E-Commerce Shootout Storefront</h1>
      <p className="text-sm text-muted-foreground">
        TanStack Start + React 19, backed by whichever of the 11 backends is
        under test via BACKEND_TARGET_URL.
      </p>
      <button
        className="mt-4 rounded-md bg-black px-4 py-2 text-white"
        onClick={() => add({ sku: "SKU-1", name: "Demo Item", unitPriceCents: 1999, quantity: 1 })}
      >
        Add demo item to cart
      </button>
    </main>
  );
}
