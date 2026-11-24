import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { createTestApp, validEnquiry, type TestApp } from './helpers.js';

let ctx: TestApp;

beforeEach(async () => {
  ctx = await createTestApp();
});

afterEach(async () => {
  await ctx.app.close();
});

function post(body: unknown, key: string | null = 'key-1') {
  return ctx.app.inject({
    method: 'POST',
    url: '/api/v1/enquiries',
    headers: key === null ? {} : { 'idempotency-key': key },
    payload: body as object,
  });
}

describe('POST /api/v1/enquiries', () => {
  it('emails the sales inbox and returns an enquiry id', async () => {
    const response = await post(validEnquiry);

    expect(response.statusCode).toBe(201);
    expect(response.json()).toEqual({ enquiryId: 'enq_test1' });
    expect(ctx.email.sent).toHaveLength(1);

    const [email] = ctx.email.sent;
    expect(email).toMatchObject({
      to: 'sales@test.local',
      replyTo: { name: 'Jane Citizen', address: 'jane@example.com' },
      subject: 'New enquiry: Mens Cotton Jacket (#3)',
    });
    expect(email?.text).toContain('Phone:    +61 400 000 000');
    expect(email?.text).toContain('Is this available in blue?');
    expect(email?.text).toContain('Price:    AUD 55.99');
    expect(email?.text).toContain('Enquiry ID: enq_test1');
  });

  it('accepts an enquiry without the optional phone and message', async () => {
    const response = await post({ productId: 3, name: 'Jane', email: 'jane@example.com', phone: null, message: '' });

    expect(response.statusCode).toBe(201);
    expect(ctx.email.sent[0]?.text).toContain('Phone:    Not provided');
    expect(ctx.email.sent[0]?.text).toContain('(No message)');
  });

  it('trims input before validating and sending', async () => {
    await post({ ...validEnquiry, name: '  Jane  ', email: ' jane@example.com ' });

    expect(ctx.email.sent[0]?.replyTo).toEqual({ name: 'Jane', address: 'jane@example.com' });
  });

  describe('validation', () => {
    it.each([
      ['name', { name: undefined }, 'REQUIRED'],
      ['name', { name: '   ' }, 'REQUIRED'],
      ['name', { name: 'a'.repeat(101) }, 'TOO_LONG'],
      ['name', { name: 'Jane\r\nBcc: x@y.z' }, 'INVALID_FORMAT'],
      ['email', { email: undefined }, 'REQUIRED'],
      ['email', { email: 'not-an-email' }, 'INVALID_FORMAT'],
      ['email', { email: `${'a'.repeat(250)}@x.io` }, 'TOO_LONG'],
      ['phone', { phone: 'call me maybe' }, 'INVALID_FORMAT'],
      ['phone', { phone: '1'.repeat(21) }, 'TOO_LONG'],
      ['message', { message: 'a'.repeat(501) }, 'TOO_LONG'],
      ['productId', { productId: undefined }, 'REQUIRED'],
      ['productId', { productId: '3' }, 'INVALID_FORMAT'],
    ])('rejects a bad %s (%j) with %s', async (field, override, code) => {
      const response = await post({ ...validEnquiry, ...override });

      expect(response.statusCode).toBe(400);
      expect(response.json()).toEqual({
        code: 'VALIDATION_ERROR',
        message: 'Some fields are invalid.',
        fieldErrors: [{ field, code }],
      });
      expect(ctx.email.sent).toHaveLength(0);
    });

    it('reports every invalid field at once, including the header', async () => {
      const response = await post({ productId: 3, email: 'nope' }, null);

      expect(response.json().fieldErrors).toEqual([
        { field: 'Idempotency-Key', code: 'REQUIRED' },
        { field: 'name', code: 'REQUIRED' },
        { field: 'email', code: 'INVALID_FORMAT' },
      ]);
    });

    it('rejects a malformed Idempotency-Key', async () => {
      const response = await post(validEnquiry, 'has spaces!');

      expect(response.json().fieldErrors).toEqual([{ field: 'Idempotency-Key', code: 'INVALID_FORMAT' }]);
    });

    it('ignores unknown fields rather than failing', async () => {
      const response = await post({ ...validEnquiry, favouriteColour: 'blue' });

      expect(response.statusCode).toBe(201);
    });
  });

  it('returns 404 and sends nothing for an unknown product', async () => {
    const response = await post({ ...validEnquiry, productId: 999 });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toMatchObject({ code: 'NOT_FOUND' });
    expect(ctx.email.sent).toHaveLength(0);
  });

  describe('idempotency', () => {
    it('replays the original response for a retry and sends only one email', async () => {
      const first = await post(validEnquiry, 'retry-key');
      const second = await post(validEnquiry, 'retry-key');

      expect(second.statusCode).toBe(201);
      expect(second.json()).toEqual(first.json());
      expect(ctx.email.sent).toHaveLength(1);
    });

    it('sends one email when the same request arrives twice concurrently', async () => {
      const [a, b] = await Promise.all([post(validEnquiry, 'race-key'), post(validEnquiry, 'race-key')]);

      expect(a.json()).toEqual(b.json());
      expect(ctx.email.sent).toHaveLength(1);
    });

    it('treats different keys as different enquiries', async () => {
      await post(validEnquiry, 'key-a');
      await post(validEnquiry, 'key-b');

      expect(ctx.email.sent).toHaveLength(2);
    });

    it('rejects a reused key with a different body', async () => {
      await post(validEnquiry, 'reused');
      const response = await post({ ...validEnquiry, message: 'Something else' }, 'reused');

      expect(response.statusCode).toBe(409);
      expect(response.json()).toMatchObject({ code: 'IDEMPOTENCY_KEY_REUSED' });
      expect(ctx.email.sent).toHaveLength(1);
    });
  });

  describe('when the mail server is down', () => {
    it('returns 502 and lets the client retry with the same key', async () => {
      ctx.email.failWith = new Error('ECONNREFUSED');
      const failed = await post(validEnquiry, 'flaky');

      expect(failed.statusCode).toBe(502);
      expect(failed.json()).toEqual({ code: 'UPSTREAM_ERROR', message: expect.any(String) });
      expect(failed.body).not.toContain('ECONNREFUSED');

      ctx.email.failWith = null;
      const retried = await post(validEnquiry, 'flaky');

      expect(retried.statusCode).toBe(201);
      expect(ctx.email.sent).toHaveLength(1);
    });
  });
});
