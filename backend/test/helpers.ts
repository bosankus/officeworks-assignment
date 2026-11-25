import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { CatalogueService } from '../src/catalogue/CatalogueService.js';
import { fakeStoreProductSchema, toProduct } from '../src/catalogue/fakestore/mapper.js';
import { applyFilter, distinctCategories } from '../src/catalogue/matching.js';
import type { Category, Product, ProductFilter, ProductRepository } from '../src/catalogue/product.js';
import type { EmailMessage, EmailSender } from '../src/enquiry/EmailSender.js';
import { EnquiryService } from '../src/enquiry/EnquiryService.js';
import { InMemoryIdempotencyGuard } from '../src/enquiry/IdempotencyGuard.js';
import fixture from './fixtures/fakestore-products.json' with { type: 'json' };

export const fakeStoreFixture = fixture;

export const fixtureProducts: Product[] = fixture.map((item) => toProduct(fakeStoreProductSchema.parse(item), 'AUD'));

/** Test double for the product source; behaves like a real one over a fixed list. */
export class InMemoryProductRepository implements ProductRepository {
  constructor(private readonly products: Product[] = fixtureProducts) {}

  async listCategories(): Promise<Category[]> {
    return distinctCategories(this.products);
  }

  async findProducts(filter?: ProductFilter): Promise<Product[]> {
    return applyFilter(this.products, filter);
  }

  async findById(id: number): Promise<Product | null> {
    return this.products.find((product) => product.id === id) ?? null;
  }
}

/** Records sent emails; set `failWith` to simulate the mail server being down. */
export class RecordingEmailSender implements EmailSender {
  readonly sent: EmailMessage[] = [];
  failWith: Error | null = null;

  async send(message: EmailMessage): Promise<void> {
    if (this.failWith) throw this.failWith;
    this.sent.push(message);
  }
}

export interface TestApp {
  app: FastifyInstance;
  email: RecordingEmailSender;
}

export async function createTestApp(
  overrides: {
    repository?: ProductRepository;
    featuredIds?: number[];
    enquiryRatePerMinute?: number;
    apiRatePerMinute?: number;
  } = {},
): Promise<TestApp> {
  const email = new RecordingEmailSender();
  const catalogue = new CatalogueService(overrides.repository ?? new InMemoryProductRepository(), overrides.featuredIds ?? [1, 5, 11, 17, 18]);
  let sequence = 0;
  const enquiries = new EnquiryService({
    catalogue,
    emailSender: email,
    idempotency: new InMemoryIdempotencyGuard(),
    salesInbox: 'sales@test.local',
    newId: () => `enq_test${++sequence}`,
    now: () => new Date('2026-11-23T10:00:00.000Z'),
  });
  const app = await buildApp({
    catalogue,
    enquiries,
    apiRatePerMinute: overrides.apiRatePerMinute ?? 1000,
    enquiryRatePerMinute: overrides.enquiryRatePerMinute ?? 100,
  });
  return { app, email };
}

export const validEnquiry = {
  productId: 3,
  name: 'Jane Citizen',
  email: 'jane@example.com',
  phone: '+61 400 000 000',
  message: 'Is this available in blue?',
};
