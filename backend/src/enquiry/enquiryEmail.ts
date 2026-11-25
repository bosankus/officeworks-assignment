import type { Product } from '../catalogue/product.js';
import type { EmailMessage } from './EmailSender.js';
import type { EnquiryInput } from './enquiry.js';

/** Plain text on purpose: nothing customer-typed is ever rendered as HTML in the sales inbox. */
export function buildEnquiryEmail(params: {
  enquiryId: string;
  enquiry: EnquiryInput;
  product: Product;
  salesInbox: string;
  receivedAt: Date;
}): EmailMessage {
  const { enquiryId, enquiry, product, salesInbox, receivedAt } = params;
  const oneLine = (text: string) => text.replace(/[\r\n]+/g, ' ');

  const text = [
    'New product enquiry',
    '',
    `Product:  ${product.title} (#${product.id})`,
    `Category: ${product.category.name}`,
    `Price:    ${product.currency} ${product.price.toFixed(2)}`,
    '',
    `Name:     ${enquiry.name}`,
    `Email:    ${enquiry.email}`,
    `Phone:    ${enquiry.phone ?? 'Not provided'}`,
    '',
    'Message:',
    enquiry.message ?? '(No message)',
    '',
    '--',
    `Enquiry ID: ${enquiryId}`,
    `Received:   ${receivedAt.toISOString()}`,
    'Reply to this email to respond to the customer directly.',
  ].join('\n');

  return {
    to: salesInbox,
    replyTo: { name: enquiry.name, address: enquiry.email },
    subject: oneLine(`New enquiry: ${product.title} (#${product.id})`),
    text,
  };
}
