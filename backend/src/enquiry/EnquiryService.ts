import { createHash, randomUUID } from 'node:crypto';
import type { CatalogueService } from '../catalogue/CatalogueService.js';
import { AppError, UpstreamError } from '../shared/errors.js';
import { silentLogger, type Logger } from '../shared/logger.js';
import type { EmailSender } from './EmailSender.js';
import type { EnquiryInput, EnquiryReceipt } from './enquiry.js';
import { buildEnquiryEmail } from './enquiryEmail.js';
import type { IdempotencyGuard } from './IdempotencyGuard.js';

export interface EnquiryServiceDeps {
  catalogue: CatalogueService;
  emailSender: EmailSender;
  idempotency: IdempotencyGuard;
  salesInbox: string;
  logger?: Logger;
  newId?: () => string;
  now?: () => Date;
}

/** Turns a validated enquiry into an email to sales, exactly once per idempotency key. */
export class EnquiryService {
  private readonly logger: Logger;
  private readonly newId: () => string;
  private readonly now: () => Date;

  constructor(private readonly deps: EnquiryServiceDeps) {
    this.logger = deps.logger ?? silentLogger;
    this.newId = deps.newId ?? (() => `enq_${randomUUID().replaceAll('-', '')}`);
    this.now = deps.now ?? (() => new Date());
  }

  submit(enquiry: EnquiryInput, idempotencyKey: string): Promise<EnquiryReceipt> {
    return this.deps.idempotency.run(idempotencyKey, fingerprint(enquiry), () => this.send(enquiry));
  }

  private async send(enquiry: EnquiryInput): Promise<EnquiryReceipt> {
    // Throws NotFoundError for an unknown product, before anything is sent.
    const product = await this.deps.catalogue.getProduct(enquiry.productId);
    const enquiryId = this.newId();

    try {
      await this.deps.emailSender.send(
        buildEnquiryEmail({ enquiryId, enquiry, product, salesInbox: this.deps.salesInbox, receivedAt: this.now() }),
      );
    } catch (error) {
      this.logger.error({ enquiryId, productId: product.id, err: error }, 'Enquiry email failed');
      // Whatever the adapter throws, the client sees a retryable 502, never a raw 500.
      throw error instanceof AppError ? error : new UpstreamError('email', { cause: error });
    }

    // Ids only: the customer's name, email, phone and message never reach the logs.
    this.logger.info({ enquiryId, productId: product.id }, 'Enquiry sent to sales');
    return { enquiryId };
  }
}

function fingerprint(enquiry: EnquiryInput): string {
  const canonical = JSON.stringify([enquiry.productId, enquiry.name, enquiry.email, enquiry.phone, enquiry.message]);
  return createHash('sha256').update(canonical).digest('hex');
}
