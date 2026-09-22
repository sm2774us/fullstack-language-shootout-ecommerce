// Required entry point for TanStack Start's Vite plugin — it resolves this
// file by convention (src/router.tsx) to build the router the SSR/client
// entries both use. `routeTree.gen.ts` is generated automatically by the
// TanStack Router Vite plugin (bundled inside @tanstack/react-start's
// plugin) from the file-based routes under src/routes/ on every build —
// it does not exist in source control and should not be committed.
import { createRouter as createTanStackRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";

export function getRouter() {
  return createTanStackRouter({
    routeTree,
    defaultPreload: "intent",
    scrollRestoration: true,
  });
}

declare module "@tanstack/react-router" {
  interface Register {
    router: ReturnType<typeof getRouter>;
  }
}
