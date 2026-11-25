import { describe, expect, it } from 'vitest';
import type { ProductRepository } from '../src/catalogue/product.js';
import { UpstreamError } from '../src/shared/errors.js';
import { createTestApp, validEnquiry } from './helpers.js';

describe('HTTP behaviour shared by all endpoints', () => {
  it('reports health outside the versioned API', async () => {
    const { app } = await createTestApp();

    const response = await app.inject({ method: 'GET', url: '/health' });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ status: 'ok' });
  });

  it('returns a contract-shaped 404 for unknown routes', async () => {
    const { app } = await createTestApp();

    const response = await app.inject({ method: 'GET', url: '/api/v1/nope' });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toEqual({ code: 'NOT_FOUND', message: 'Route not found.' });
  });

  it('returns 400 for a malformed JSON body', async () => {
    const { app } = await createTestApp();

    const response = await app.inject({
      method: 'POST',
      url: '/api/v1/enquiries',
      headers: { 'content-type': 'application/json', 'idempotency-key': 'k' },
      payload: '{"name":',
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({ code: 'VALIDATION_ERROR' });
  });

  it('returns 502 without leaking details when the product source is down', async () => {
    const failing: ProductRepository = {
      listCategories: () => Promise.reject(new UpstreamError('fakestore', { cause: new Error('secret internal detail') })),
      findProducts: () => Promise.reject(new UpstreamError('fakestore')),
      findById: () => Promise.reject(new UpstreamError('fakestore')),
    };
    const { app } = await createTestApp({ repository: failing });

    const response = await app.inject({ method: 'GET', url: '/api/v1/categories' });

    expect(response.statusCode).toBe(502);
    expect(response.json()).toEqual({ code: 'UPSTREAM_ERROR', message: expect.any(String) });
    expect(response.body).not.toContain('secret');
  });

  it('returns a contract-shaped 500 for unexpected errors', async () => {
    const broken: ProductRepository = {
      listCategories: () => Promise.reject(new TypeError('boom')),
      findProducts: () => Promise.reject(new TypeError('boom')),
      findById: () => Promise.reject(new TypeError('boom')),
    };
    const { app } = await createTestApp({ repository: broken });

    const response = await app.inject({ method: 'GET', url: '/api/v1/categories' });

    expect(response.statusCode).toBe(500);
    expect(response.json()).toEqual({ code: 'INTERNAL_ERROR', message: expect.any(String) });
    expect(response.body).not.toContain('boom');
  });

  describe('rate limiting', () => {
    it('limits enquiries more tightly than browsing, with a contract-shaped 429', async () => {
      const { app, email } = await createTestApp({ enquiryRatePerMinute: 2 });
      const send = (key: string) =>
        app.inject({ method: 'POST', url: '/api/v1/enquiries', headers: { 'idempotency-key': key }, payload: validEnquiry });

      expect((await send('a')).statusCode).toBe(201);
      expect((await send('b')).statusCode).toBe(201);
      const limited = await send('c');

      expect(limited.statusCode).toBe(429);
      expect(limited.json()).toEqual({ code: 'RATE_LIMITED', message: expect.any(String) });
      expect(limited.headers['retry-after']).toBeDefined();
      expect(email.sent).toHaveLength(2);

      // Browsing is unaffected.
      expect((await app.inject({ method: 'GET', url: '/api/v1/categories' })).statusCode).toBe(200);
    });

    it('applies an API-wide limit to browsing', async () => {
      const { app } = await createTestApp({ apiRatePerMinute: 1 });

      expect((await app.inject({ method: 'GET', url: '/api/v1/categories' })).statusCode).toBe(200);
      expect((await app.inject({ method: 'GET', url: '/api/v1/categories' })).statusCode).toBe(429);
      expect((await app.inject({ method: 'GET', url: '/health' })).statusCode).toBe(200);
    });
  });
});
