import nodemailer, { type Transporter } from 'nodemailer';
import { UpstreamError } from '../shared/errors.js';
import type { EmailMessage, EmailSender } from './EmailSender.js';

export interface SmtpOptions {
  host: string;
  port: number;
  secure: boolean;
  user?: string;
  password?: string;
  from: string;
}

/** Works with Mailpit locally and with any SMTP relay (SES SMTP, SendGrid SMTP) in other environments. */
export class SmtpEmailSender implements EmailSender {
  private readonly transporter: Transporter;

  constructor(private readonly options: SmtpOptions) {
    this.transporter = nodemailer.createTransport({
      host: options.host,
      port: options.port,
      secure: options.secure,
      auth: options.user ? { user: options.user, pass: options.password ?? '' } : undefined,
      connectionTimeout: 5_000,
      greetingTimeout: 5_000,
      socketTimeout: 10_000,
    });
  }

  async send(message: EmailMessage): Promise<void> {
    try {
      await this.transporter.sendMail({
        from: this.options.from,
        to: message.to,
        replyTo: message.replyTo,
        subject: message.subject,
        text: message.text,
      });
    } catch (error) {
      throw new UpstreamError('smtp', { cause: error });
    }
  }

  close(): void {
    this.transporter.close();
  }
}
