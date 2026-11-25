import type { FastifyPluginAsync } from 'fastify';
import { toFieldErrors } from '../http/validation.js';
import { ValidationError } from '../shared/errors.js';
import { enquiryInputSchema, idempotencyKeySchema } from './enquiry.js';
import type { EnquiryService } from './EnquiryService.js';

export interface EnquiryRouteOptions {
  ratePerMinute: number;
}

export const enquiryRoutes =
  (enquiries: EnquiryService, options: EnquiryRouteOptions): FastifyPluginAsync =>
  async (app) => {
    app.post(
      '/enquiries',
      // Tighter than the API-wide limit: each accepted request sends a real email.
      { config: { rateLimit: { max: options.ratePerMinute, timeWindow: '1 minute' } } },
      async (request, reply) => {
        // Validate header and body together so the client gets every problem in one response.
        const key = idempotencyKeySchema.safeParse(request.headers['idempotency-key'], { reportInput: true });
        const body = enquiryInputSchema.safeParse(request.body ?? {}, { reportInput: true });
        if (!key.success || !body.success) {
          throw new ValidationError([
            ...(key.success ? [] : toFieldErrors(key.error, 'Idempotency-Key')),
            ...(body.success ? [] : toFieldErrors(body.error)),
          ]);
        }

        const receipt = await enquiries.submit(body.data, key.data);
        return reply.status(201).send(receipt);
      },
    );
  };
