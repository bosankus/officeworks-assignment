import rateLimit from '@fastify/rate-limit';
import Fastify, { type FastifyBaseLogger, type FastifyInstance } from 'fastify';
import type { CatalogueService } from './catalogue/CatalogueService.js';
import { catalogueRoutes } from './catalogue/routes.js';
import type { EnquiryService } from './enquiry/EnquiryService.js';
import { enquiryRoutes } from './enquiry/routes.js';
import { registerErrorHandling } from './http/errorHandler.js';

export interface AppDeps {
  catalogue: CatalogueService;
  enquiries: EnquiryService;
  apiRatePerMinute: number;
  enquiryRatePerMinute: number;
  /** Shared with the services so every log line goes through one configured pino instance. Omit to disable logging. */
  logger?: FastifyBaseLogger;
  trustProxy?: boolean;
}

/** Builds the HTTP app from already-constructed services, so tests can swap any dependency. */
export async function buildApp(deps: AppDeps): Promise<FastifyInstance> {
  const app = Fastify({
    ...(deps.logger ? { loggerInstance: deps.logger } : { logger: false }),
    trustProxy: deps.trustProxy ?? false,
    bodyLimit: 16 * 1024,
  });

  registerErrorHandling(app);

  await app.register(rateLimit, {
    global: true,
    max: deps.apiRatePerMinute,
    timeWindow: '1 minute',
    errorResponseBuilder: (_request, context) => ({ statusCode: context.statusCode, message: 'Rate limited' }),
  });

  app.get('/health', { config: { rateLimit: false } }, () => ({ status: 'ok' }));

  await app.register(
    async (api) => {
      await api.register(catalogueRoutes(deps.catalogue));
      await api.register(enquiryRoutes(deps.enquiries, { ratePerMinute: deps.enquiryRatePerMinute }));
    },
    { prefix: '/api/v1' },
  );

  return app;
}
