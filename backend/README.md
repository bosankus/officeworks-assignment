# Catalogue backend

Node.js + TypeScript API that the Android app talks to. It implements [API_CONTRACT.md](../API_CONTRACT.md)
over the Fake Store API and emails enquiries to the sales inbox. The app never calls Fake Store directly.

## Run

### Docker (recommended)

From the repo root:

```bash
docker compose up --build
```

| What | Where |
|---|---|
| API | http://localhost:3000/api/v1/products/featured |
| Sales inbox (Mailpit) | http://localhost:8025 |
| Health | http://localhost:3000/health |
| From the Android emulator | `http://10.0.2.2:3000/api/v1/` |

No accounts or secrets are needed. Optional overrides go in `backend/.env` (copy `.env.example`).

### Without Docker

Needs Node 22+.

```bash
cd backend
cp .env.example .env
npm install
npm run dev          # http://localhost:3000, reloads on change
```

Enquiry emails need an SMTP server on `localhost:1025`. Without Docker, run `brew install mailpit` and
then `mailpit` in its own terminal tab, and leave it running. With Docker: `docker compose up mailpit`.
Without one, enquiries return `502 UPSTREAM_ERROR`, which is the designed behaviour when email is down.

### Try it

```bash
curl localhost:3000/api/v1/products/featured
curl localhost:3000/api/v1/categories
curl localhost:3000/api/v1/categories/electronics/products
curl "localhost:3000/api/v1/products?query=jacket"
curl localhost:3000/api/v1/products/3
curl -X POST localhost:3000/api/v1/enquiries \
  -H 'content-type: application/json' -H "idempotency-key: $(uuidgen)" \
  -d '{"productId":3,"name":"Jane Citizen","email":"jane@example.com","message":"Is this available in blue?"}'
```

Then open http://localhost:8025 to see the email.

`$(uuidgen)` only works in a shell. In Postman or another REST client, type a key yourself (e.g.
`test-key-1`): letters, digits, `-` and `_` only. Use a new key for each new enquiry; reusing one with a
different body returns `409`.

## Test

```bash
npm test               # no network needed
npm run check          # typecheck + lint + tests with coverage floor: what CI runs
npm run test:coverage  # coverage report in coverage/
```

CI (`.github/workflows/backend.yml`) runs `npm run check` and a build on every PR that touches the
backend. It then starts the stack with `docker compose up` exactly as above, sends an enquiry twice with
the same idempotency key, and checks that exactly one email reached Mailpit.

Tests run the real Fastify app in-process (`app.inject`) against a recorded Fake Store fixture, a
recording email sender, and a tiny in-process SMTP server for the SMTP adapter.

## Configuration

All optional; see [.env.example](.env.example) for every key and its default. The ones you're most
likely to change:

| Key | Default | Meaning |
|---|---|---|
| `FEATURED_PRODUCT_IDS` | `1,5,11,17,18` | The owner's "top rated" picks for the home page, in order (max 5). |
| `SALES_INBOX` | `sales@catalogue.local` | Where enquiries are sent. |
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` | Any SMTP server: Mailpit locally, e.g. Amazon SES SMTP in AWS. |
| `CATALOGUE_CACHE_TTL_SECONDS` | `300` | How long the product catalogue is cached. |
| `TRUST_PROXY` | `false` | Set `true` behind a load balancer so rate limits use the client IP. |

Invalid configuration stops the server at startup with every problem listed.

## Structure

```
src/
  server.ts                 Composition root: the only file that picks implementations
  app.ts                    Fastify app: plugins, rate limits, routes under /api/v1
  config.ts                 Env parsing and validation (zod)
  catalogue/
    product.ts              Our Product/Category model and the ProductRepository port
    CatalogueService.ts     Featured, categories, search, product rules
    matching.ts             Slugs, display names, in-memory search for sources that can't search
    routes.ts               HTTP <-> service, input validation
    fakestore/              Fake Store adapter: fetch, stale-while-revalidate cache, map vendor DTOs
  enquiry/
    EnquiryService.ts       Validated enquiry -> email to sales, once per idempotency key
    EmailSender.ts          Email port
    SmtpEmailSender.ts      SMTP adapter (nodemailer)
    IdempotencyGuard.ts     At-most-once per Idempotency-Key (in memory)
    enquiry.ts              Request schema (the contract's rules)
    enquiryEmail.ts         Email content
    routes.ts
  http/                     Contract-shaped errors and zod -> field error mapping
  shared/                   Error types, logger interface
```

Dependencies point inwards: routes -> services -> ports (`ProductRepository`, `EmailSender`,
`IdempotencyGuard`). Adapters implement ports; only `server.ts` knows which ones are used.

## Swapping things out

| Change | What to write |
|---|---|
| Real product database / PIM / search index | A class implementing `ProductRepository`, then one line in `server.ts`. Push filtering down to the source; `ProductFilter` is already in the interface for that. |
| Email provider (SES API, SendGrid, a queue) | A class implementing `EmailSender`. SMTP relays work today with config only. |
| More than one instance | `IdempotencyGuard` and the rate-limit store move to Redis or DynamoDB; the interfaces stay the same. |
