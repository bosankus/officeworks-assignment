import type { Category, Product, ProductFilter } from './product.js';

/** "Men's Clothing" -> "men's clothing"; apostrophes dropped so "mens" finds "men's". */
function normalise(text: string): string {
  return text.toLowerCase().replace(/['’]/g, '');
}

/** Lowercase, URL-safe, stable across vendors: "men's clothing" -> "mens-clothing". */
export function toSlug(name: string): string {
  return normalise(name)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

/** Vendor category names are lowercase ("men's clothing"); show them in sentence case. */
export function toDisplayName(name: string): string {
  const trimmed = name.trim();
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

function words(text: string): string[] {
  return normalise(text).split(/[^a-z0-9]+/).filter(Boolean);
}

/**
 * Every search term must be the start of some word: "jack" finds "jacket", but "mens" doesn't find
 * "womens" (plain substring matching did, which tests caught).
 */
function matchesText(product: Product, text: string): boolean {
  const terms = words(text);
  if (terms.length === 0) return true;
  const productWords = words(`${product.title} ${product.description} ${product.category.name}`);
  return terms.every((term) => productWords.some((word) => word.startsWith(term)));
}

/** In-memory filtering for sources that can't filter themselves. */
export function applyFilter(products: Product[], filter: ProductFilter = {}): Product[] {
  return products.filter(
    (product) =>
      (filter.categorySlug === undefined || product.category.slug === filter.categorySlug) &&
      (filter.text === undefined || matchesText(product, filter.text)),
  );
}

export function distinctCategories(products: Product[]): Category[] {
  const bySlug = new Map<string, Category>();
  for (const { category } of products) bySlug.set(category.slug, category);
  return [...bySlug.values()].sort((a, b) => a.name.localeCompare(b.name));
}
