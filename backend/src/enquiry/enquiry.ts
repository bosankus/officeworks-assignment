import { z } from 'zod';

// Matching control characters is the point: they'd let a name inject extra lines into the email.
// eslint-disable-next-line no-control-regex
const CONTROL_CHARS = /[\u0000-\u001f\u007f]/;
const PHONE_PATTERN = /^[+0-9 ()-]+$/;

/** Rules from API_CONTRACT.md. The app mirrors them, but this is the source of truth. */
export const enquiryInputSchema = z.object({
  productId: z.number().int().positive(),
  name: z
    .string()
    .trim()
    .min(1)
    .max(100)
    .refine((value) => !CONTROL_CHARS.test(value)),
  email: z.string().trim().min(1).max(254).pipe(z.email()),
  phone: z
    .string()
    .trim()
    .max(20)
    .refine((value) => value === '' || PHONE_PATTERN.test(value))
    .nullish()
    .transform((value) => value || undefined),
  message: z
    .string()
    .trim()
    .max(500)
    .nullish()
    .transform((value) => value || undefined),
});

export type EnquiryInput = z.output<typeof enquiryInputSchema>;

/** Generated once per enquiry by the client and reused on retries. */
export const idempotencyKeySchema = z
  .string()
  .trim()
  .min(1)
  .max(100)
  .regex(/^[A-Za-z0-9_-]+$/);

export interface EnquiryReceipt {
  enquiryId: string;
}
