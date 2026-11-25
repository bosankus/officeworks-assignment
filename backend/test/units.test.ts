import { describe, expect, it } from 'vitest';
import { toDisplayName, toSlug } from '../src/catalogue/matching.js';
import { loadConfig } from '../src/config.js';
import { InMemoryIdempotencyGuard } from '../src/enquiry/IdempotencyGuard.js';
import { IdempotencyConflictError } from '../src/shared/errors.js';

describe('category naming', () => {
  it.each([
    ["men's clothing", 'mens-clothing'],
    ['Women’s Clothing', 'womens-clothing'],
    ['  Home & Garden ', 'home-garden'],
    ['electronics', 'electronics'],
  ])('slugs %j as %j', (name, slug) => {
    expect(toSlug(name)).toBe(slug);
  });

  it('shows vendor names in sentence case', () => {
    expect(toDisplayName("men's clothing")).toBe("Men's clothing");
  });
});

describe('loadConfig', () => {
  it('provides working local defaults', () => {
    const config = loadConfig({});

    expect(config).toMatchObject({
      PORT: 3000,
      FAKESTORE_BASE_URL: 'https://fakestoreapi.com',
      FEATURED_PRODUCT_IDS: [1, 5, 11, 17, 18],
      SMTP_HOST: 'localhost',
      SMTP_PORT: 1025,
      SMTP_SECURE: false,
      TRUST_PROXY: false,
    });
  });

  it('parses featured ids and booleans from strings, treating empty values as unset', () => {
    const config = loadConfig({ FEATURED_PRODUCT_IDS: ' 3, 4 ,9', SMTP_SECURE: 'true', SMTP_USER: '' });

    expect(config.FEATURED_PRODUCT_IDS).toEqual([3, 4, 9]);
    expect(config.SMTP_SECURE).toBe(true);
    expect(config.SMTP_USER).toBeUndefined();
  });

  it('fails fast listing every bad variable', () => {
    expect(() => loadConfig({ PORT: 'abc', FEATURED_PRODUCT_IDS: '1,2,3,4,5,6', SALES_INBOX: 'nope' })).toThrow(
      /PORT[\s\S]*FEATURED_PRODUCT_IDS[\s\S]*SALES_INBOX/,
    );
  });

  it('rejects non-numeric featured ids', () => {
    expect(() => loadConfig({ FEATURED_PRODUCT_IDS: '1,abc' })).toThrow(/FEATURED_PRODUCT_IDS/);
  });
});

describe('InMemoryIdempotencyGuard', () => {
  it('forgets keys after the TTL', async () => {
    let now = 0;
    const guard = new InMemoryIdempotencyGuard({ ttlMs: 1000, now: () => now });
    let calls = 0;
    const op = async () => ++calls;

    await guard.run('k', 'f', op);
    await guard.run('k', 'f', op);
    expect(calls).toBe(1);

    now = 1000;
    await guard.run('k', 'f', op);
    expect(calls).toBe(2);
  });

  it('forgets the oldest keys once it reaches its size limit', async () => {
    const guard = new InMemoryIdempotencyGuard({ maxEntries: 2 });
    let calls = 0;
    const op = async () => ++calls;

    await guard.run('a', 'f', op);
    await guard.run('b', 'f', op);
    await guard.run('c', 'f', op); // evicts 'a'
    await guard.run('b', 'f', op); // still remembered
    expect(calls).toBe(3);

    await guard.run('a', 'f', op); // forgotten, so it runs again
    expect(calls).toBe(4);
  });

  it('rejects a different fingerprint for the same key', async () => {
    const guard = new InMemoryIdempotencyGuard();
    await guard.run('k', 'f1', async () => 1);

    await expect(guard.run('k', 'f2', async () => 2)).rejects.toBeInstanceOf(IdempotencyConflictError);
  });
});
