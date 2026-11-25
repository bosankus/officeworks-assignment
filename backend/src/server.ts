import { pino } from 'pino';
import { buildApp } from './app.js';
import { CatalogueService } from './catalogue/CatalogueService.js';
import { FakeStoreProductRepository } from './catalogue/fakestore/FakeStoreProductRepository.js';
import { loadConfig } from './config.js';
import { EnquiryService } from './enquiry/EnquiryService.js';
import { InMemoryIdempotencyGuard } from './enquiry/IdempotencyGuard.js';
import { SmtpEmailSender } from './enquiry/SmtpEmailSender.js';

/**
 * Composition root: the only file that picks concrete implementations. Swapping the product source
 * or email provider means changing a line here, not the services or routes.
 */
async function main(): Promise<void> {
  const config = loadConfig();
  const logger = pino({ level: config.LOG_LEVEL });

  const products = new FakeStoreProductRepository({
    baseUrl: config.FAKESTORE_BASE_URL,
    currency: config.CURRENCY,
    timeoutMs: config.FAKESTORE_TIMEOUT_MS,
    cacheTtlMs: config.CATALOGUE_CACHE_TTL_SECONDS * 1000,
    logger,
  });
  const catalogue = new CatalogueService(products, config.FEATURED_PRODUCT_IDS, logger);

  const emailSender = new SmtpEmailSender({
    host: config.SMTP_HOST,
    port: config.SMTP_PORT,
    secure: config.SMTP_SECURE,
    user: config.SMTP_USER,
    password: config.SMTP_PASSWORD,
    from: config.ENQUIRY_FROM,
  });
  const enquiries = new EnquiryService({
    catalogue,
    emailSender,
    idempotency: new InMemoryIdempotencyGuard(),
    salesInbox: config.SALES_INBOX,
    logger,
  });

  const app = await buildApp({
    catalogue,
    enquiries,
    apiRatePerMinute: config.API_RATE_LIMIT_PER_MINUTE,
    enquiryRatePerMinute: config.ENQUIRY_RATE_LIMIT_PER_MINUTE,
    logger,
    trustProxy: config.TRUST_PROXY,
  });

  const shutdown = async (signal: string) => {
    logger.info({ signal }, 'Shutting down');
    await app.close();
    emailSender.close();
    process.exit(0);
  };
  process.once('SIGINT', () => void shutdown('SIGINT'));
  process.once('SIGTERM', () => void shutdown('SIGTERM'));

  await app.listen({ host: config.HOST, port: config.PORT });
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});
