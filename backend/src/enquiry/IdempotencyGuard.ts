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

export interface InMemoryIdempotencyOptions {
  /** How long a key is remembered. */
  ttlMs?: number;
  /** Upper bound on remembered keys, so memory can't grow without limit; the oldest go first. */
  maxEntries?: number;
  now?: () => number;
}

/**
 * Single-process only. With more than one instance this becomes a conditional write to Redis or
 * DynamoDB behind the same interface.
 */
export class InMemoryIdempotencyGuard implements IdempotencyGuard {
  private readonly entries = new Map<string, Entry>();
  private readonly ttlMs: number;
  private readonly maxEntries: number;
  private readonly now: () => number;

  constructor(options: InMemoryIdempotencyOptions = {}) {
    this.ttlMs = options.ttlMs ?? 24 * 60 * 60 * 1000;
    this.maxEntries = options.maxEntries ?? 10_000;
    this.now = options.now ?? Date.now;
  }

  run<T>(key: string, fingerprint: string, operation: () => Promise<T>): Promise<T> {
    this.evictExpired();

    const existing = this.entries.get(key);
    if (existing) {
      if (existing.fingerprint !== fingerprint) return Promise.reject(new IdempotencyConflictError());
      return existing.result as Promise<T>;
    }

    // Make room only when adding a key, so a retry can never evict the entry it's looking for.
    while (this.entries.size >= this.maxEntries) {
      const oldest = this.entries.keys().next().value;
      if (oldest === undefined) break;
      this.entries.delete(oldest);
    }

    const result = operation();
    this.entries.set(key, { fingerprint, result, expiresAt: this.now() + this.ttlMs });
    result.catch(() => {
      if (this.entries.get(key)?.result === result) this.entries.delete(key);
    });
    return result;
  }

  /** Every key gets the same TTL and Maps keep insertion order, so expired entries are always at the front. */
  private evictExpired(): void {
    const now = this.now();
    for (const [key, entry] of this.entries) {
      if (entry.expiresAt > now) break;
      this.entries.delete(key);
    }
  }
}
