import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';
import { parseOrThrow } from '../http/validation.js';
import type { CatalogueService } from './CatalogueService.js';

const searchQuerySchema = z.object({ query: z.string().trim().min(1).max(100) });
const productIdSchema = z.object({ id: z.coerce.number().int().positive() });
const categoryParamsSchema = z.object({ slug: z.string().min(1).max(100) });

export const catalogueRoutes =
  (catalogue: CatalogueService): FastifyPluginAsync =>
  async (app) => {
    app.get('/products/featured', () => catalogue.getFeatured());

    app.get('/categories', () => catalogue.listCategories());

    app.get('/categories/:slug/products', (request) => {
      const { slug } = parseOrThrow(categoryParamsSchema, request.params);
      return catalogue.getCategoryProducts(slug);
    });

    app.get('/products', (request) => {
      const { query } = parseOrThrow(searchQuerySchema, request.query);
      return catalogue.search(query);
    });

    app.get('/products/:id', (request) => {
      const { id } = parseOrThrow(productIdSchema, request.params);
      return catalogue.getProduct(id);
    });
  };
