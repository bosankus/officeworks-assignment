/**
 * Our own product model. It is shaped by API_CONTRACT.md, not by any vendor, so a new data source
 * only needs a new mapper.
 */
export interface Category {
  slug: string;
  name: string;
}

export interface Product {
  id: number;
  title: string;
  description: string;
  price: number;
  currency: string;
  category: Category;
  imageUrl: string;
  rating: { rate: number; count: number };
}

export interface ProductFilter {
  categorySlug?: string;
  /** Free text; every whitespace-separated term must match somewhere in the product. */
  text?: string;
}

/**
 * The boundary between the catalogue and wherever products come from (Fake Store today; a database,
 * PIM or search index later). Filtering is part of the interface so a real source can do it natively
 * instead of us loading everything.
 */
export interface ProductRepository {
  listCategories(): Promise<Category[]>;
  findProducts(filter?: ProductFilter): Promise<Product[]>;
  findById(id: number): Promise<Product | null>;
}
