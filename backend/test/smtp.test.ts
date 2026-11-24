import { createServer, type AddressInfo, type Server } from 'node:net';
import { afterEach, describe, expect, it } from 'vitest';
import { SmtpEmailSender } from '../src/enquiry/SmtpEmailSender.js';
import { UpstreamError } from '../src/shared/errors.js';

/** Just enough SMTP to accept one plain-text message and hand back the raw DATA. */
function startSmtpServer(): Promise<{ server: Server; port: number; received: Promise<string> }> {
  let resolveReceived: (data: string) => void;
  const received = new Promise<string>((resolve) => (resolveReceived = resolve));

  const server = createServer((socket) => {
    let inData = false;
    let data = '';
    let buffer = '';
    socket.write('220 test ESMTP\r\n');
    socket.on('data', (chunk) => {
      buffer += chunk.toString();
      let index: number;
      while ((index = buffer.indexOf('\r\n')) >= 0) {
        const line = buffer.slice(0, index);
        buffer = buffer.slice(index + 2);
        if (inData) {
          if (line === '.') {
            inData = false;
            resolveReceived(data);
            socket.write('250 OK\r\n');
          } else {
            data += `${line}\n`;
          }
        } else if (/^(EHLO|HELO)/i.test(line)) socket.write('250 test\r\n');
        else if (/^DATA/i.test(line)) {
          inData = true;
          socket.write('354 go ahead\r\n');
        } else if (/^QUIT/i.test(line)) socket.end('221 bye\r\n');
        else socket.write('250 OK\r\n');
      }
    });
  });

  return new Promise((resolve) =>
    server.listen(0, '127.0.0.1', () => resolve({ server, port: (server.address() as AddressInfo).port, received })),
  );
}

describe('SmtpEmailSender', () => {
  let server: Server | undefined;

  afterEach(() => {
    server?.close();
  });

  it('delivers a plain-text email with the customer as Reply-To', async () => {
    const smtp = await startSmtpServer();
    server = smtp.server;
    const sender = new SmtpEmailSender({ host: '127.0.0.1', port: smtp.port, secure: false, from: 'Catalogue <no-reply@test.local>' });

    await sender.send({
      to: 'sales@test.local',
      replyTo: { name: 'Jane Citizen', address: 'jane@example.com' },
      subject: 'New enquiry: Mens Cotton Jacket (#3)',
      text: 'Hello sales',
    });
    sender.close();

    const raw = await smtp.received;
    expect(raw).toContain('To: sales@test.local');
    expect(raw).toContain('Reply-To: Jane Citizen <jane@example.com>');
    expect(raw).toContain('Subject: New enquiry: Mens Cotton Jacket (#3)');
    expect(raw).toContain('Content-Type: text/plain');
    expect(raw).toContain('Hello sales');
  });

  it('wraps connection failures in UpstreamError', async () => {
    // Grab a free port, then close it so nothing is listening.
    const probe = await startSmtpServer();
    probe.server.close();
    const sender = new SmtpEmailSender({ host: '127.0.0.1', port: probe.port, secure: false, from: 'x@test.local' });

    await expect(
      sender.send({ to: 'a@test.local', replyTo: { name: 'A', address: 'a@test.local' }, subject: 's', text: 't' }),
    ).rejects.toBeInstanceOf(UpstreamError);
  });
});
