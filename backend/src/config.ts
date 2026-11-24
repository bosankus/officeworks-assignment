import { z } from 'zod';

const csvOfIds = z
  .string()
  .transform((raw) =>
    raw
      .split(',')
      .map((part) => part.trim())
      .filter((part) => part.length > 0)
      .map(Number),
  )
  .pipe(z.array(z.number().int().positive()).max(5, 'At most 5 featured products'));

const booleanString = z
  .enum(['true', 'false'])
  .transform((value) => value === 'true');

const envSchema = z.object({
  HOST: z.string().default('0.0.0.0'),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),
  LOG_LEVEL: z.enum(['fatal', 'error', 'warn', 'info', 'debug', 'trace', 'silent']).default('info'),
  /** Set true behind a load balancer (e.g. AWS ALB) so rate limits key on the client IP, not the LB's. */
  TRUST_PROXY: booleanString.default(false),

  FAKESTORE_BASE_URL: z.url().default('https://fakestoreapi.com'),
  FAKESTORE_TIMEOUT_MS: z.coerce.number().int().positive().default(5000),
  CATALOGUE_CACHE_TTL_SECONDS: z.coerce.number().int().min(0).default(300),
  CURRENCY: z.string().length(3).default('AUD'),
  FEATURED_PRODUCT_IDS: csvOfIds.default([1, 5, 11, 17, 18]),

  SMTP_HOST: z.string().default('localhost'),
  SMTP_PORT: z.coerce.number().int().min(1).max(65535).default(1025),
  SMTP_SECURE: booleanString.default(false),
  SMTP_USER: z.string().optional(),
  SMTP_PASSWORD: z.string().optional(),
  SALES_INBOX: z.email().default('sales@catalogue.local'),
  ENQUIRY_FROM: z.string().default('Catalogue <no-reply@catalogue.local>'),

  ENQUIRY_RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(5),
  API_RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(300),
});

export type Config = z.infer<typeof envSchema>;

/** Fails fast at startup with every bad variable listed, rather than at the first request that needs it. */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  // Treat empty strings as unset so `KEY=` in a .env file falls back to the default.
  const cleaned = Object.fromEntries(Object.entries(env).filter(([, value]) => value !== ''));
  const result = envSchema.safeParse(cleaned);
  if (!result.success) {
    const problems = result.error.issues.map((issue) => `  ${issue.path.join('.')}: ${issue.message}`);
    throw new Error(`Invalid configuration:\n${problems.join('\n')}`);
  }
  return result.data;
}
