import { NotFoundError } from '../shared/errors.js';
import { silentLogger, type Logger } from '../shared/logger.js';
import type { Category, Product, ProductRepository } from './product.js';

/** Business rules for browsing. Knows nothing about HTTP or about which vendor supplies products. */
export class CatalogueService {
  constructor(
    private readonly repository: ProductRepository,
    private readonly featuredProductIds: readonly number[],
    private readonly logger: Logger = silentLogger,
  ) {}

  /** The owner's curated "top rated" picks, in the configured order. Missing ids are skipped, not fatal. */
  async getFeatured(): Promise<Product[]> {
    const products = await Promise.all(this.featuredProductIds.map((id) => this.repository.findById(id)));
    const missing = this.featuredProductIds.filter((_, index) => products[index] === null);
    if (missing.length > 0) {
      this.logger.warn({ missing }, 'Featured product ids not found in the product source');
    }
    return products.filter((product): product is Product => product !== null);
  }

  listCategories(): Promise<Category[]> {
    return this.repository.listCategories();
  }

  async getCategoryProducts(slug: string): Promise<Product[]> {
    const categories = await this.repository.listCategories();
    if (!categories.some((category) => category.slug === slug)) {
      throw new NotFoundError(`Category '${slug}' was not found.`);
    }
    return this.repository.findProducts({ categorySlug: slug });
  }

  search(query: string): Promise<Product[]> {
    return this.repository.findProducts({ text: query });
  }

  async getProduct(id: number): Promise<Product> {
    const product = await this.repository.findById(id);
    if (!product) throw new NotFoundError(`Product ${id} was not found.`);
    return product;
  }
}
