import type { FastifyInstance } from 'fastify';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { Product } from '../src/catalogue/product.js';
import { createTestApp, InMemoryProductRepository } from './helpers.js';

let app: FastifyInstance;

beforeEach(async () => {
  ({ app } = await createTestApp());
});

afterEach(async () => {
  await app.close();
});

const get = (url: string) => app.inject({ method: 'GET', url });
const ids = (products: Product[]) => products.map((product) => product.id);

describe('GET /api/v1/products/featured (home page)', () => {
  it('returns the 5 configured products in the configured order', async () => {
    const response = await get('/api/v1/products/featured');

    expect(response.statusCode).toBe(200);
    expect(ids(response.json())).toEqual([1, 5, 11, 17, 18]);
  });

  it('returns products in the contract shape', async () => {
    const [first] = (await get('/api/v1/products/featured')).json();

    expect(first).toEqual({
      id: 1,
      title: 'Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops',
      description: expect.any(String),
      price: 109.95,
      currency: 'AUD',
      category: { slug: 'mens-clothing', name: "Men's clothing" },
      imageUrl: 'https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_t.png',
      rating: { rate: 3.9, count: 120 },
    });
  });

  it('skips configured ids the product source no longer has', async () => {
    await app.close();
    ({ app } = await createTestApp({ featuredIds: [1, 999, 3] }));

    expect(ids((await get('/api/v1/products/featured')).json())).toEqual([1, 3]);
  });
});

describe('GET /api/v1/categories', () => {
  it('returns every category with a URL-safe slug, sorted by name', async () => {
    const response = await get('/api/v1/categories');

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual([
      { slug: 'electronics', name: 'Electronics' },
      { slug: 'jewelery', name: 'Jewelery' },
      { slug: 'mens-clothing', name: "Men's clothing" },
      { slug: 'womens-clothing', name: "Women's clothing" },
    ]);
  });
});

describe('GET /api/v1/categories/:slug/products (category page)', () => {
  it('returns every product in the category and nothing else', async () => {
    const response = await get('/api/v1/categories/mens-clothing/products');

    expect(response.statusCode).toBe(200);
    const products: Product[] = response.json();
    expect(ids(products).sort((a, b) => a - b)).toEqual([1, 2, 3, 4]);
    expect(products.every((product) => product.category.slug === 'mens-clothing')).toBe(true);
  });

  it('includes ratings so the app can pick the top rated', async () => {
    const products: Product[] = (await get('/api/v1/categories/electronics/products')).json();

    expect(products.every((product) => typeof product.rating.rate === 'number')).toBe(true);
  });

  it('returns 404 for an unknown category', async () => {
    const response = await get('/api/v1/categories/garden/products');

    expect(response.statusCode).toBe(404);
    expect(response.json()).toMatchObject({ code: 'NOT_FOUND' });
  });

  it('returns an empty list for a known category with no products', async () => {
    await app.close();
    const repository = new InMemoryProductRepository();
    repository.findProducts = async () => [];
    ({ app } = await createTestApp({ repository }));

    const response = await get('/api/v1/categories/electronics/products');
    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual([]);
  });
});

describe('GET /api/v1/products?query= (search)', () => {
  const search = (query: string) => get(`/api/v1/products?query=${encodeURIComponent(query)}`);

  it('only returns products whose text matches the term', async () => {
    const response = await search('jacket');

    expect(response.statusCode).toBe(200);
    expect(ids(response.json()).sort((a, b) => a - b)).toEqual([3, 15, 16, 17]);
  });

  it('is case-insensitive', async () => {
    expect(ids((await search('SSD')).json())).toEqual(ids((await search('ssd')).json()));
    expect((await search('SSD')).json()).toHaveLength(2);
  });

  it('matches on description and category name, not just title', async () => {
    expect(ids((await search('mythical water dragon')).json())).toEqual([5]);
    expect(ids((await search('jewelery')).json()).sort((a, b) => a - b)).toEqual([5, 6, 7, 8]);
  });

  it('matches whole words or word prefixes, so "mens" does not find "womens"', async () => {
    expect(ids((await search('mens')).json()).sort((a, b) => a - b)).toEqual([1, 2, 3, 4]);
    expect(ids((await search('jack')).json()).sort((a, b) => a - b)).toEqual([3, 15, 16, 17]);
  });

  it('requires every term to match, ignoring apostrophes', async () => {
    expect(ids((await search('mens cotton')).json())).toEqual([3]);
    expect(ids((await search("women's jacket")).json()).sort((a, b) => a - b)).toEqual([15, 16, 17]);
  });

  it('returns 200 with an empty list when nothing matches', async () => {
    const response = await search('zzzz-no-such-thing');

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual([]);
  });

  it.each([
    ['missing', '/api/v1/products', 'REQUIRED'],
    ['blank', '/api/v1/products?query=%20%20', 'REQUIRED'],
    ['too long', `/api/v1/products?query=${'a'.repeat(101)}`, 'TOO_LONG'],
  ])('rejects a %s query with 400', async (_label, url, code) => {
    const response = await get(url);

    expect(response.statusCode).toBe(400);
    expect(response.json()).toEqual({
      code: 'VALIDATION_ERROR',
      message: expect.any(String),
      fieldErrors: [{ field: 'query', code }],
    });
  });
});

describe('GET /api/v1/products/:id (product page)', () => {
  it('returns the product', async () => {
    const response = await get('/api/v1/products/3');

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({ id: 3, title: 'Mens Cotton Jacket', price: 55.99 });
  });

  it('returns 404 for an unknown id', async () => {
    const response = await get('/api/v1/products/999');

    expect(response.statusCode).toBe(404);
    expect(response.json()).toMatchObject({ code: 'NOT_FOUND' });
  });

  it.each(['abc', '0', '-1', '1.5'])('returns 400 for a malformed id (%s)', async (id) => {
    const response = await get(`/api/v1/products/${id}`);

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({ code: 'VALIDATION_ERROR', fieldErrors: [{ field: 'id' }] });
  });
});
