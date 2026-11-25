import type { z } from 'zod';
import { ValidationError, type FieldError, type FieldErrorCode } from '../shared/errors.js';

function toFieldErrorCode(issue: z.core.$ZodIssue): FieldErrorCode {
  switch (issue.code) {
    case 'invalid_type':
      return issue.input === undefined || issue.input === null ? 'REQUIRED' : 'INVALID_FORMAT';
    case 'too_small':
      // Every min(1) in our schemas means "must be present"; anything else is a format problem.
      return issue.minimum === 1 && issue.origin === 'string' ? 'REQUIRED' : 'INVALID_FORMAT';
    case 'too_big':
      return 'TOO_LONG';
    default:
      return 'INVALID_FORMAT';
  }
}

/** Maps zod issues onto the contract's field error codes; one error per field, first issue wins. */
export function toFieldErrors(error: z.ZodError, prefix?: string): FieldError[] {
  const byField = new Map<string, FieldErrorCode>();
  for (const issue of error.issues) {
    const path = issue.path.map(String).join('.');
    const field = prefix ? (path ? `${prefix}.${path}` : prefix) : path;
    if (!byField.has(field)) byField.set(field, toFieldErrorCode(issue));
  }
  return [...byField].map(([field, code]) => ({ field, code }));
}

/** Parses or throws a contract-shaped ValidationError. `field` names a single value, e.g. a header. */
export function parseOrThrow<S extends z.ZodType>(schema: S, value: unknown, field?: string): z.output<S> {
  const result = schema.safeParse(value, { reportInput: true });
  if (!result.success) throw new ValidationError(toFieldErrors(result.error, field));
  return result.data;
}
