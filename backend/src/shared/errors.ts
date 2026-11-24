/** Error codes are part of API_CONTRACT.md; clients branch on them, so treat them as public API. */
export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'NOT_FOUND'
  | 'IDEMPOTENCY_KEY_REUSED'
  | 'RATE_LIMITED'
  | 'UPSTREAM_ERROR'
  | 'INTERNAL_ERROR';

export type FieldErrorCode = 'REQUIRED' | 'INVALID_FORMAT' | 'TOO_LONG';

export interface FieldError {
  field: string;
  code: FieldErrorCode;
}

export abstract class AppError extends Error {
  abstract readonly statusCode: number;
  abstract readonly code: ErrorCode;
}

export class ValidationError extends AppError {
  readonly statusCode = 400;
  readonly code = 'VALIDATION_ERROR' as const;

  constructor(readonly fieldErrors: FieldError[]) {
    super('Some fields are invalid.');
  }
}

export class NotFoundError extends AppError {
  readonly statusCode = 404;
  readonly code = 'NOT_FOUND' as const;
}

/** Same Idempotency-Key sent with a different body: almost certainly a client bug, so refuse rather than guess. */
export class IdempotencyConflictError extends AppError {
  readonly statusCode = 409;
  readonly code = 'IDEMPOTENCY_KEY_REUSED' as const;

  constructor() {
    super('This Idempotency-Key was already used for a different request.');
  }
}

/** A dependency we don't control (product source, mail server) failed or timed out. */
export class UpstreamError extends AppError {
  readonly statusCode = 502;
  readonly code = 'UPSTREAM_ERROR' as const;

  constructor(
    readonly upstream: string,
    options?: { cause?: unknown },
  ) {
    super('A service we depend on is unavailable. Please try again.', options);
  }
}
