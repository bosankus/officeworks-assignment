import { describe, expect, it, vi } from 'vitest';
import { FakeStoreProductRepository } from '../src/catalogue/fakestore/FakeStoreProductRepository.js';
import { toProduct } from '../src/catalogue/fakestore/mapper.js';
import { UpstreamError } from '../src/shared/errors.js';
import { fakeStoreFixture } from './helpers.js';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
}

function setup(options: { ttlMs?: number; fetch?: typeof fetch } = {}) {
  let now = 0;
  const fetchMock = vi.fn(options.fetch ?? (async () => jsonResponse(fakeStoreFixture)));
  const repository = new FakeStoreProductRepository({
    baseUrl: 'https://fakestore.test',
    currency: 'AUD',
    timeoutMs: 1000,
    cacheTtlMs: options.ttlMs ?? 60_000,
    fetch: fetchMock,
    now: () => now,
  });
  return { repository, fetchMock, advance: (ms: number) => (now += ms) };
}

describe('Fake Store mapper', () => {
  it('maps vendor fields onto our product model', () => {
    const product = toProduct(
      {
        id: 2,
        title: ' Mens Casual Premium Slim Fit T-Shirts ',
        price: 22.3,
        description: 'Soft. ',
        category: "men's clothing",
        image: 'https://img/2.jpg',
        rating: { rate: 4.1, count: 259 },
      },
      'AUD',
    );

    expect(product).toEqual({
      id: 2,
      title: 'Mens Casual Premium Slim Fit T-Shirts',
      description: 'Soft.',
      price: 22.3,
      currency: 'AUD',
      category: { slug: 'mens-clothing', name: "Men's clothing" },
      imageUrl: 'https://img/2.jpg',
      rating: { rate: 4.1, count: 259 },
    });
  });

  it('rounds prices to cents', () => {
    const product = toProduct(
      { id: 1, title: 't', price: 0.1 + 0.2, description: '', category: 'x', image: 'i', rating: { rate: 1, count: 1 } },
      'AUD',
    );
    expect(product.price).toBe(0.3);
  });
});

describe('FakeStoreProductRepository', () => {
  it('requests /products from the configured base URL', async () => {
    const { repository, fetchMock } = setup();

    await repository.findProducts();

    expect(fetchMock).toHaveBeenCalledWith('https://fakestore.test/products', expect.anything());
  });

  it('keeps a path prefix in the base URL', async () => {
    const fetchMock = vi.fn<typeof fetch>(async () => jsonResponse(fakeStoreFixture));
    const repository = new FakeStoreProductRepository({
      baseUrl: 'https://proxy.test/fakestore/',
      currency: 'AUD',
      timeoutMs: 1000,
      cacheTtlMs: 0,
      fetch: fetchMock,
    });

    await repository.findProducts();

    expect(fetchMock).toHaveBeenCalledWith('https://proxy.test/fakestore/products', expect.anything());
  });

  it('finds by id and returns null for unknown ids', async () => {
    const { repository } = setup();

    expect((await repository.findById(3))?.title).toBe('Mens Cotton Jacket');
    expect(await repository.findById(999)).toBeNull();
  });

  it('derives categories from products', async () => {
    const { repository } = setup();

    expect((await repository.listCategories()).map((category) => category.slug)).toEqual([
      'electronics',
      'jewelery',
      'mens-clothing',
      'womens-clothing',
    ]);
  });

  it('caches the catalogue for the TTL, then refreshes', async () => {
    const { repository, fetchMock, advance } = setup({ ttlMs: 1000 });

    await repository.findProducts();
    await repository.listCategories();
    await repository.findById(1);
    expect(fetchMock).toHaveBeenCalledTimes(1);

    advance(1000);
    await repository.findProducts();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('shares one upstream request between concurrent cache misses', async () => {
    const { repository, fetchMock } = setup();

    await Promise.all([repository.findProducts(), repository.listCategories(), repository.findById(1)]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('serves the stale catalogue when a refresh fails', async () => {
    let fail = false;
    const { repository, advance } = setup({
      ttlMs: 1000,
      fetch: async () => (fail ? jsonResponse({}, 503) : jsonResponse(fakeStoreFixture)),
    });
    await repository.findProducts();

    fail = true;
    advance(5000);

    expect(await repository.findProducts()).toHaveLength(20);
  });

  it('answers from the stale catalogue immediately while Fake Store is slow or down', async () => {
    let hang = false;
    const { repository, fetchMock, advance } = setup({
      ttlMs: 1000,
      fetch: (async () => (hang ? new Promise<Response>(() => {}) : jsonResponse(fakeStoreFixture))),
    });
    await repository.findProducts();

    hang = true;
    advance(5000);
    const result = await Promise.race([
      repository.findProducts(),
      new Promise((resolve) => setTimeout(() => resolve('blocked'), 50)),
    ]);
    expect(result).toHaveLength(20);

    // Further requests share the one in-flight refresh instead of piling onto Fake Store.
    await repository.findProducts();
    await repository.listCategories();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('uses the refreshed catalogue once the background refresh completes', async () => {
    let version = 1;
    const { repository, advance } = setup({
      ttlMs: 1000,
      fetch: async () => jsonResponse(version === 1 ? fakeStoreFixture : fakeStoreFixture.slice(0, 3)),
    });
    await repository.findProducts();

    version = 2;
    advance(1000);
    expect(await repository.findProducts()).toHaveLength(20); // stale answer, refresh started
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(await repository.findProducts()).toHaveLength(3);
  });

  it.each([
    ['a non-2xx response', async () => jsonResponse({ error: 'nope' }, 500)],
    ['a network error', async () => Promise.reject(new TypeError('fetch failed'))],
    ['a non-array body', async () => jsonResponse({ products: [] })],
    ['invalid JSON', async () => new Response('<html>', { status: 200 })],
  ])('throws UpstreamError on %s with nothing cached', async (_label, failingFetch) => {
    const { repository } = setup({ fetch: failingFetch });

    await expect(repository.findProducts()).rejects.toBeInstanceOf(UpstreamError);
  });

  it('skips malformed records instead of failing the whole catalogue', async () => {
    const { repository } = setup({
      fetch: async () => jsonResponse([fakeStoreFixture[0], { id: 'oops' }, fakeStoreFixture[1]]),
    });

    expect((await repository.findProducts()).map((product) => product.id)).toEqual([1, 2]);
  });

  it('times out a hanging upstream', async () => {
    const repository = new FakeStoreProductRepository({
      baseUrl: 'https://fakestore.test',
      currency: 'AUD',
      timeoutMs: 20,
      cacheTtlMs: 0,
      // Never answers; only the timeout signal can end it, just like a hung connection.
      fetch: ((_url: string, init?: RequestInit) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () => reject(new Error('aborted by timeout')));
        })) as typeof fetch,
    });

    await expect(repository.findProducts()).rejects.toBeInstanceOf(UpstreamError);
  });
});
