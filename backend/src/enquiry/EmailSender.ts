export interface EmailMessage {
  to: string;
  replyTo: { name: string; address: string };
  subject: string;
  text: string;
}

/** Port for outbound email. SMTP today; SES, SendGrid or a queue later, without touching the enquiry logic. */
export interface EmailSender {
  send(message: EmailMessage): Promise<void>;
}
