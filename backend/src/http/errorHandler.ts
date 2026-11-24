import type { FastifyError, FastifyInstance } from 'fastify';
import { AppError, ValidationError, type ErrorCode, type FieldError } from '../shared/errors.js';

interface ErrorBody {
  code: ErrorCode;
  message: string;
  fieldErrors?: FieldError[];
}

/** Every error leaves the API in the contract's shape; internals and stack traces never do. */
export function registerErrorHandling(app: FastifyInstance): void {
  app.setNotFoundHandler((_request, reply) => {
    reply.status(404).send({ code: 'NOT_FOUND', message: 'Route not found.' } satisfies ErrorBody);
  });

  app.setErrorHandler((error: FastifyError | AppError, request, reply) => {
    if (error instanceof ValidationError) {
      return reply
        .status(error.statusCode)
        .send({ code: error.code, message: error.message, fieldErrors: error.fieldErrors } satisfies ErrorBody);
    }

    if (error instanceof AppError) {
      if (error.statusCode >= 500) request.log.error({ err: error, cause: error.cause }, error.message);
      return reply.status(error.statusCode).send({ code: error.code, message: error.message } satisfies ErrorBody);
    }

    // Raised by @fastify/rate-limit via its errorResponseBuilder.
    if (error.statusCode === 429) {
      return reply
        .status(429)
        .send({ code: 'RATE_LIMITED', message: 'Too many requests. Please try again shortly.' } satisfies ErrorBody);
    }

    // Fastify's own 4xx: malformed JSON, unsupported content type, body too large.
    if (error.statusCode !== undefined && error.statusCode >= 400 && error.statusCode < 500) {
      return reply
        .status(400)
        .send({ code: 'VALIDATION_ERROR', message: 'The request could not be read.', fieldErrors: [] } satisfies ErrorBody);
    }

    request.log.error({ err: error }, 'Unhandled error');
    return reply
      .status(500)
      .send({ code: 'INTERNAL_ERROR', message: 'Something went wrong. Please try again.' } satisfies ErrorBody);
  });
}
