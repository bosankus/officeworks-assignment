import { UpstreamError } from '../../shared/errors.js';
import { silentLogger, type Logger } from '../../shared/logger.js';
import { applyFilter, distinctCategories } from '../matching.js';
import type { Category, Product, ProductFilter, ProductRepository } from '../product.js';
import { fakeStoreProductSchema, toProduct } from './mapper.js';

export interface FakeStoreOptions {
  baseUrl: string;
  currency: string;
  timeoutMs: number;
  /** 0 disables caching. */
  cacheTtlMs: number;
  fetch?: typeof globalThis.fetch;
  now?: () => number;
  logger?: Logger;
}

interface Snapshot {
  products: Product[];
  loadedAt: number;
}

/**
 * Fake Store has ~20 products and no search, so we load the whole catalogue once, cache it, and
 * filter in memory. That also sidesteps its quirk of answering unknown ids with 200 and an empty body.
 */
export class FakeStoreProductRepository implements ProductRepository {
  private snapshot: Snapshot | null = null;
  private inFlight: Promise<Snapshot> | null = null;
  private readonly fetch: typeof globalThis.fetch;
  private readonly now: () => number;
  private readonly logger: Logger;

  constructor(private readonly options: FakeStoreOptions) {
    this.fetch = options.fetch ?? globalThis.fetch;
    this.now = options.now ?? Date.now;
    this.logger = options.logger ?? silentLogger;
  }

  async listCategories(): Promise<Category[]> {
    return distinctCategories(await this.products());
  }

  async findProducts(filter?: ProductFilter): Promise<Product[]> {
    return applyFilter(await this.products(), filter);
  }

  async findById(id: number): Promise<Product | null> {
    return (await this.products()).find((product) => product.id === id) ?? null;
  }

  /**
   * Stale-while-revalidate: once a catalogue has loaded, requests never wait on Fake Store again. An
   * expired catalogue is served immediately while one background refresh replaces it; if that refresh
   * fails, the old catalogue stays and the next request tries again.
   */
  private async products(): Promise<Product[]> {
    const current = this.snapshot;
    if (!current) return (await this.refresh()).products;

    if (this.now() - current.loadedAt >= this.options.cacheTtlMs) {
      this.refresh().catch((error: unknown) => {
        this.logger.warn({ err: error, ageMs: this.now() - current.loadedAt }, 'Catalogue refresh failed; serving stale data');
      });
    }
    return current.products;
  }

  /** Concurrent cache misses share one upstream request. */
  private refresh(): Promise<Snapshot> {
    this.inFlight ??= this.load().finally(() => {
      this.inFlight = null;
    });
    return this.inFlight;
  }

  private async load(): Promise<Snapshot> {
    // Join rather than `new URL('/products', base)`, which would drop a path prefix such as a proxy's.
    const url = `${this.options.baseUrl.replace(/\/+$/, '')}/products`;
    let body: unknown;
    try {
      const response = await this.fetch(url, {
        headers: { accept: 'application/json' },
        signal: AbortSignal.timeout(this.options.timeoutMs),
      });
      if (!response.ok) throw new Error(`Fake Store responded ${response.status}`);
      body = await response.json();
    } catch (error) {
      throw new UpstreamError('fakestore', { cause: error });
    }

    if (!Array.isArray(body)) {
      throw new UpstreamError('fakestore', { cause: new Error('Expected an array of products') });
    }

    const products: Product[] = [];
    for (const item of body) {
      const parsed = fakeStoreProductSchema.safeParse(item);
      if (parsed.success) {
        products.push(toProduct(parsed.data, this.options.currency));
      } else {
        // One malformed record shouldn't take the catalogue down.
        this.logger.warn({ productId: (item as { id?: unknown })?.id, issues: parsed.error.issues.length }, 'Skipping invalid Fake Store product');
      }
    }

    const snapshot = { products, loadedAt: this.now() };
    this.snapshot = snapshot;
    this.logger.info({ count: products.length }, 'Catalogue loaded from Fake Store');
    return snapshot;
  }
}
