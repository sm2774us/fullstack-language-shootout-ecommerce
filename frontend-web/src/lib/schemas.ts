// Zod schemas mirroring proto/ecommerce.proto request/response shapes.
import { z } from "zod";

export const orderLineItemSchema = z.object({
  sku: z.string(),
  quantity: z.number().int().positive(),
  unitPriceCents: z.number().int().nonnegative(),
});

export const createOrderSchema = z.object({
  customerId: z.string(),
  items: z.array(orderLineItemSchema).min(1),
});

export type CreateOrderInput = z.infer<typeof createOrderSchema>;
