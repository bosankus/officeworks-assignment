import { IdempotencyConflictError } from '../shared/errors.js';

/**
 * Runs an operation at most once per key. A repeat with the same key and payload gets the original
 * result (or joins the in-flight attempt); a repeat with a different payload is rejected.
 * Failures are forgotten so the client can retry with the same key.
 */
export interface IdempotencyGuard {
  run<T>(key: string, fingerprint: string, operation: () => Promise<T>): Promise<T>;
}

interface Entry {
  fingerprint: string;
  result: Promise<unknown>;
  expiresAt: number;
}

/**
 * Single-process only. With more than one instance this becomes a conditional write to Redis or
 * DynamoDB behind the same interface.
 */
export class InMemoryIdempotencyGuard implements IdempotencyGuard {
  private readonly entries = new Map<string, Entry>();

  constructor(
    private readonly ttlMs: number = 24 * 60 * 60 * 1000,
    private readonly now: () => number = Date.now,
  ) {}

  run<T>(key: string, fingerprint: string, operation: () => Promise<T>): Promise<T> {
    this.evictExpired();

    const existing = this.entries.get(key);
    if (existing) {
      if (existing.fingerprint !== fingerprint) return Promise.reject(new IdempotencyConflictError());
      return existing.result as Promise<T>;
    }

    const result = operation();
    this.entries.set(key, { fingerprint, result, expiresAt: this.now() + this.ttlMs });
    result.catch(() => {
      if (this.entries.get(key)?.result === result) this.entries.delete(key);
    });
    return result;
  }

  private evictExpired(): void {
    const now = this.now();
    for (const [key, entry] of this.entries) {
      if (entry.expiresAt <= now) this.entries.delete(key);
    }
  }
}
