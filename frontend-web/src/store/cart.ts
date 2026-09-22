// Zustand cart store — client state only; server truth lives in the
// orders module via gRPC-Web through src/server/*.
import { create } from "zustand";

export interface CartLine {
  sku: string;
  name: string;
  unitPriceCents: number;
  quantity: number;
}

interface CartState {
  lines: CartLine[];
  add: (line: CartLine) => void;
  remove: (sku: string) => void;
  totalCents: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  lines: [],
  add: (line) =>
    set((state) => {
      const existing = state.lines.find((l) => l.sku === line.sku);
      if (existing) {
        existing.quantity += line.quantity;
        return { lines: [...state.lines] };
      }
      return { lines: [...state.lines, line] };
    }),
  remove: (sku) => set((state) => ({ lines: state.lines.filter((l) => l.sku !== sku) })),
  totalCents: () => get().lines.reduce((sum, l) => sum + l.unitPriceCents * l.quantity, 0),
}));
