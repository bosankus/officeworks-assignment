# API contract (v1)

The contract between the app and our backend ([`backend/`](backend/README.md)). The app's `core:data`
tests parse responses captured from this backend, so changes here should be re-captured
(`scripts/capture-contract-fixtures.sh`).

The backend wraps the Fake Store API. The app never calls Fake Store directly.

- Base path: `/api/v1`
- JSON, UTF-8. Unknown fields must be ignored by clients.
- Breaking changes ship as `/api/v2`.

## Models

### Product

```json
{
  "id": 1,
  "title": "Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops",
  "description": "Your perfect pack for everyday use...",
  "price": 109.95,
  "currency": "AUD",
  "category": { "slug": "mens-clothing", "name": "Men's clothing" },
  "imageUrl": "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_t.png",
  "rating": { "rate": 3.9, "count": 120 }
}
```

### Category

```json
{ "slug": "mens-clothing", "name": "Men's clothing" }
```

Slugs are URL-safe. Fake Store category names (`men's clothing`) are display names only.

### Error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Some fields are invalid.",
  "fieldErrors": [{ "field": "email", "code": "INVALID_FORMAT" }]
}
```

| HTTP | `code` |
|---|---|
| 400 | `VALIDATION_ERROR` |
| 404 | `NOT_FOUND` |
| 409 | `IDEMPOTENCY_KEY_REUSED` |
| 429 | `RATE_LIMITED` |
| 500 | `INTERNAL_ERROR` |
| 502 | `UPSTREAM_ERROR` (product source or mail server unavailable; safe to retry) |

`fieldErrors[].code` is one of `REQUIRED`, `INVALID_FORMAT`, `TOO_LONG`. `fieldErrors` is only present
on `VALIDATION_ERROR`. Messages are for developers; clients should branch on `code`.

Any endpoint can return `500`, `429` (with a `Retry-After` header; 300 requests a minute per IP) or `502`
(if the product source is down and the backend has no cached catalogue).

## Endpoints

### `GET /products/featured`

The 5 products the owner has chosen as "top rated", in display order.
The list is curated in backend config; it is not computed from ratings.

`200` -> `Product[]`: up to 5. Fewer if fewer are configured, or if a configured id no longer exists
in the product source (it's skipped and logged rather than failing the page).

### `GET /categories`

`200` -> `Category[]`, sorted by name.

### `GET /categories/{slug}/products`

All products in the category. Ordering is not guaranteed; the app sorts.

`200` -> `Product[]` · `404` unknown slug

### `GET /products?query={text}`

Case-insensitive match on title, description and category name. Every word in the query must match
the start of a word in the product: `jack` finds "jacket", `mens` does not find "womens".
Apostrophes are ignored, so `mens` finds "men's".

- `query` is required, 1–100 characters after trimming.
- No match returns `200` with `[]`, not 404.

`200` -> `Product[]` · `400` missing or invalid query

### `GET /products/{id}`

`200` -> `Product` · `404` unknown id

Fake Store returns `200` with an empty body for unknown ids; the backend must turn that into `404`.

### `POST /enquiries`

Sends an email to the sales inbox.

Headers: `Idempotency-Key: <uuid>`, generated once per enquiry by the app. Required; 1–100 characters
of letters, digits, `-` and `_`.

- A retry with the same key and body returns the original response and does not send a second email.
- The same key with a different body returns `409 IDEMPOTENCY_KEY_REUSED`.
- If the email can't be sent, the response is `502` and the key is released, so retry with the same key.
- Keys are remembered for 24 hours.

```json
{
  "productId": 1,
  "name": "Jane Citizen",
  "email": "jane@example.com",
  "phone": "+61 400 000 000",
  "message": "Is this available in blue?"
}
```

| Field | Rule |
|---|---|
| `productId` | required, must exist |
| `name` | required, 1–100 chars, no control characters |
| `email` | required, valid format, max 254 chars |
| `phone` | optional, max 20 chars of digits, spaces and `+ ( ) -` |
| `message` | optional, max 500 chars |

Optional fields may be omitted, `null` or empty. All strings are trimmed before validation.

`201` -> `{ "enquiryId": "enq_8f2c..." }` · `400` validation · `404` unknown product ·
`409` key reused with a different body · `429` rate limited (5 per minute per IP) · `502` email not sent

The app validates the same rules before sending; the backend is still the source of truth.

### `GET /health`

Outside `/api/v1`, not rate limited. For container and load balancer health checks.

`200` -> `{ "status": "ok" }`

## Not in v1

Pagination, sorting parameters, product create/update/delete (Fake Store's admin endpoints don't
persist and aren't needed for a customer-facing catalogue), auth.
