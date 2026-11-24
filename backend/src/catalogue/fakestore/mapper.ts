import { z } from 'zod';
import { toDisplayName, toSlug } from '../matching.js';
import type { Product } from '../product.js';

/** Only the fields we use. Unknown fields are ignored so vendor additions don't break us. */
export const fakeStoreProductSchema = z.object({
  id: z.number().int().positive(),
  title: z.string().min(1),
  price: z.number().nonnegative(),
  description: z.string(),
  category: z.string().min(1),
  image: z.string().min(1),
  rating: z
    .object({ rate: z.number().min(0).max(5), count: z.number().int().nonnegative() })
    .default({ rate: 0, count: 0 }),
});

export type FakeStoreProduct = z.infer<typeof fakeStoreProductSchema>;

export function toProduct(dto: FakeStoreProduct, currency: string): Product {
  return {
    id: dto.id,
    title: dto.title.trim(),
    description: dto.description.trim(),
    // Fake Store sends floats like 22.3; round to cents so clients never see 22.299999.
    price: Math.round(dto.price * 100) / 100,
    currency,
    category: { slug: toSlug(dto.category), name: toDisplayName(dto.category) },
    imageUrl: dto.image,
    rating: dto.rating,
  };
}
