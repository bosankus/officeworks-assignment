# Decisions and assumptions

## Stack

- **Client: Android (Kotlin)**, the only client in this submission.
- **Kotlin Multiplatform structure, Android target only.** The brief's iOS option is Swift or
  Objective-C, and a KMP iOS target is Kotlin, so shipping one could read as not meeting it. I removed
  the iOS target and kept the KMP layout: shared code lives in `commonMain`, which the compiler stops
  from using Android APIs, so the code stays iOS-ready.
- **Backend: Node.js + TypeScript** with Fastify, zod and Vitest (see "Backend" below). The app talks
  only to the backend, at `officeworks.baseUrl`.
- **No demo data in the app.** An early bundled copy of the catalogue was removed once the backend
  existed: it could drift from the real data and hide integration problems. UI work without a backend
  uses the stateless `XxxContent` composables with test data instead.

## Architecture

- **Clean Architecture + MVVM with unidirectional data flow.** One immutable `UiState` per screen, sealed
  events in. Full MVI (reducers, intent/action/result pipelines) is more ceremony than five screens need;
  a reducer can be added inside any ViewModel that grows complex without changing the architecture.
- **Use cases on every path**, even thin ones. That costs a few small classes but gives every
  feature change an obvious home and keeps ViewModels free of repository details.
- **Interfaces where implementations change, not everywhere.** Data sources and repositories are interfaces
  (the domain owns `ProductRepository`, the data layer provides `ProductRepositoryImpl`), because that's
  where the vendor, cache or backend will change. Use cases are concrete classes: each has one
  implementation, tests run them for real over fake repositories, and an interface per use case would add
  a file and a Koin binding each without making anything swappable. Extracting one when a second
  implementation appears is a small refactor.
- **Six modules** (`core:domain`, `core:data`, `core:ui`, `feature:catalogue`, `feature:enquiry`,
  `shared`). Enough to enforce boundaries at compile time without a module per screen.
- **UI tests run on an emulator.** `commonTest` is also compiled into Android host tests (plain JVM),
  where Compose UI tests can't render, so they live in `androidDeviceTest`. They use `runComposeUiTest`
  (the v2 API) rather than JUnit rules, so moving them to `commonTest` for iOS is mostly a file move.
- **No coverage number for the app.** Kover 0.9.1 doesn't support AGP 9's KMP Android library plugin, so
  it isn't wired in, and I won't quote a percentage I can't measure. Tests target the critical path
  instead: every use case, mapper, repository, error mapping and ViewModel has unit tests, and every
  acceptance criterion has a UI test. The backend does report coverage (`npm run test:coverage`).
- **HTTP errors the app distinguishes**: `Network` (offline), `Server` (500, and 502 when the catalogue
  or mail server is down; safe to retry), `RateLimited` (429), `NotFound` (404), `Validation` (400). Each
  has its own message, mapped once in `core:ui` (`DataError.toUiError`). A 400 only becomes `Validation`
  when it names a field the form shows; the data layer translates the API's field names and codes, so the
  domain never sees wire strings. Errors on anything else (the idempotency key, a bad body) are `Unknown`. **409 `IDEMPOTENCY_KEY_REUSED` maps to `Unknown`** and is logged as an error: it
  only happens if the app reuses a key for a different enquiry, which is a bug the customer can't fix.
- **Contract fixtures from the real backend.** `core:data` tests parse JSON captured from the running
  backend (`scripts/capture-contract-fixtures.sh`) into domain models, so a change in the backend's
  JSON breaks a test rather than the app.
- **Category filtering on the home page, not a separate screen.** Selecting a category keeps the chips and
  search in view, which made it easier to compare categories. The category screen, its ViewModel and route
  were removed rather than left unused.
- **Compose performance.** UI models and states are `@Immutable`; grid items have stable keys and content
  types; ViewModels hand over display-ready values, so composables don't map or format. Coil loads each
  image at the size it's displayed.
- **`DataResult` instead of `kotlin.Result`**: typed errors, and no swallowed `CancellationException`.
- **Money as `Long` cents** to avoid floating-point rounding in prices.

## App libraries

All stable versions, pinned in `gradle/libs.versions.toml`.

| Library | Why |
|---|---|
| Compose Multiplatform, Material 3, Lifecycle ViewModel | UI and screen state that stay in `commonMain`. |
| Navigation Compose (JetBrains) | Type-safe routes; the only place features are connected. |
| Koin | DI that works in KMP; Hilt and Dagger don't. |
| Ktor client (OkHttp engine) | KMP HTTP client; Retrofit is JVM-only. |
| kotlinx.serialization | DTOs and navigation routes. |
| Coil 3 (Ktor network) | Multiplatform image loading over the same HTTP stack. |
| Kermit | KMP logging to Logcat; levels by build type. |
| kotlin-test, kotlinx-coroutines-test, Turbine | Shared unit tests and `StateFlow` assertions. |
| MockK | Interaction checks in Android host tests (JVM only). |
| Ktor client mock | HTTP tests without a server. |
| Compose UI test, AndroidX test runner | UI tests on an emulator. The AndroidX `ui-test` libraries are pinned to the Compose UI version Compose Multiplatform ships (1.12.1), so tests run on the same Compose as the app. Bump them together. |
| Espresso core 3.7.0 | Declared directly because Compose UI test brings in an older Espresso that crashes on API 34+. |
| Spotless + ktlint (build only) | One formatting and lint check for every module, run first in `scripts/ci.sh`. Rules live in `.editorconfig`, so Android Studio and CI agree. `intellij_idea` style because it matches Android Studio's formatter (`ktlint_official` would rewrap ~770 lines for layout alone). Trailing commas and explicit imports are enforced. Only two exceptions, each commented: PascalCase for composables, and underscores in device test names (DEX doesn't allow spaces). |
| Compose rules 0.6.8 (ktlint ruleset, build only) | Compose-specific checks in the same `spotlessCheck`: modifier parameters with defaults, parameter order, effect keys and `rememberUpdatedState`, unstable collections, no ViewModels passed down. Turning it on found 7 issues, all fixed, including a `LaunchedEffect` that could call a stale callback. |

## Backend

- **Fastify over Express or NestJS.** Built-in structured logging (pino), fast in-process tests with
  `app.inject`, and a plugin model for rate limiting. NestJS's DI and modules are more ceremony than six
  endpoints need; the same boundaries come from plain interfaces and one composition root (`server.ts`).
- **zod for input and config.** One schema per input gives the type and the contract's field error codes.
  Config is validated at startup, so a bad env var fails fast with every problem listed.
- **Ports and adapters.** `ProductRepository`, `EmailSender` and `IdempotencyGuard` are interfaces; Fake
  Store, SMTP and in-memory are today's adapters. Routes and services don't know which are used.
- **Filtering is part of `ProductRepository`** (`findProducts({ categorySlug, text })`), so a real
  database or search index can filter natively. The Fake Store adapter filters in memory because Fake
  Store has no search.
- **Whole-catalogue cache in the Fake Store adapter, stale-while-revalidate.** Only the very first request
  waits for Fake Store. After the 5-minute TTL, requests are answered from the old catalogue at once while
  a single background refresh replaces it; if Fake Store is down, the old catalogue stays and requests
  never wait on its timeout. With ~20 products that's one request per 5 minutes, and it sidesteps Fake
  Store's 200-with-empty-body for unknown ids.
- **SMTP, with Mailpit locally.** No paid account for the reviewer, and the email is visible at
  `localhost:8025`. Amazon SES works over SMTP with config only. The email is plain text, so nothing the
  customer typed is rendered as HTML, and the customer is the `Reply-To`.
- **Idempotent enquiries.** Same key and body replays the original `201`, including concurrent duplicates;
  same key with a different body is `409`; a failed send releases the key so the app can retry.
- **Rate limits per IP**: 5 enquiries a minute (each sends a real email), 300 requests a minute overall.
- **One error shape.** Upstream failures are `502 UPSTREAM_ERROR` with no internal detail; anything
  unexpected is `500 INTERNAL_ERROR` and logged.
- **No PII in logs**: enquiry logs carry the enquiry id and product id only.
- **Two ways to run it.** `docker compose up` is the one-command path for reviewers. Plain Node plus
  Mailpit works without Docker; Docker Desktop needs a paid licence at larger companies, so it isn't required.
- **TypeScript 6.0, not 7.** TypeScript 7 (the native compiler) is out, but typescript-eslint doesn't support
  it yet. Type-aware linting (floating promises, unsafe `any`) is worth more here than a faster compiler.
- **Backend CI** (`.github/workflows/backend.yml`): typecheck, lint, tests with a coverage floor, build,
  then a smoke test that runs the reviewers' `docker compose up`, sends an enquiry twice with one key and
  checks exactly one email reached Mailpit. If Fake Store itself is down, the live checks warn rather than
  fail, because its outages aren't our regressions.
- **Verified on a real machine, not just in tests**: live Fake Store data through the backend, an
  enquiry email in Mailpit, a `502` while Mailpit was stopped, and exactly one email after the retry.

## Assumptions

| Area | Assumption |
|---|---|
| Home | The owner's 5 "top rated" products are a curated list from `GET /products/featured`, not computed. |
| Refresh | Pull-to-refresh on Home reloads the featured products, categories and any selected category while the current products stay on screen. A failed refresh keeps them and shows a snackbar instead of an error page. The backend caches the catalogue for 5 minutes, so a pull can return the same data; that's fine for a catalogue and protects Fake Store. |
| Category | Category chips filter the home page in place: each chip has a category icon, outlined by default and filled when selected; tapping the selected chip again returns to the featured products. "Top rated" = the 3 highest-rated products in that category (rating, then review count); the rest follow, sorted by rating. |
| Search | Searches all categories, case-insensitively, on title, description and category. Every term must match the start of a word, so "mens" doesn't return women's products. Blank queries are blocked in the app and rejected by the backend. |
| Search results | Open on their own screen, so back returns to Home exactly as it was. Categories filter in place because comparing them is the point; a search is a separate task. Showing results in place on Home instead would remove the second search field, and is a small change: `SearchField` and `ProductGrid` are already shared. |
| Enquiry form | Name and email required; phone and message (≤ 500 chars) optional. Product attached automatically and shown at the top (photo, title, price), so the customer can see what they're asking about. The photo and price load separately; if that fails, the title from navigation is enough and the form still works. The send button is pinned above the keyboard. Tapping the product card goes back to the product: the form is only opened from the product page. |
| After enquiry | A thank-you message replaces the form and names the email the reply will go to; double submission is prevented. The top bar is hidden there: "Back to product" or system back leaves. |
| Currency | The backend sends AUD (Fake Store has no currency field). The app formats whatever code it receives with the platform's number formatter and the device locale: `$109.95` in Australia, `A$109.95` elsewhere so it isn't mistaken for US dollars, and the right decimals for any currency. An unknown code still shows as `XYZ 5.00`. |
| Images | The backend passes Fake Store's image URLs through, and Coil loads them directly from `fakestoreapi.com`. They're static assets, not product data; every API call goes to our backend. A CDN or image proxy is a later concern. |
| Stock | No stock data exists, so every product can be enquired about. |
| Layout | Phone first; the grid column count adapts to width so tablets are usable. Styled after a reference shot (pill search, rounded-rectangle category chips drawn at 36dp with a 48dp tap area, image tiles, price badge on detail). Elements with no data or feature behind them (location, notifications, "See all", bottom navigation, 360° view, image thumbnails, chat/offer) were left out rather than shipped as dead buttons. Product tiles keep a title and price under the image so products can be told apart. |
| Language | English only, but all strings are in resources. |
| Unknown product id | Backend returns 404 (Fake Store returns 200 with an empty body). |
| Featured products | Set by `FEATURED_PRODUCT_IDS` (default `1,5,11,17,18`, spanning every category). An id the source no longer has is skipped and logged rather than breaking the home page. |
| Sales inbox | One configured address (`SALES_INBOX`). The customer is the email's Reply-To so sales can answer directly. |
| Enquiry storage | Enquiries are emailed, not stored. |

## Dropped or simplified

| What | Why |
|---|---|
| Storing enquiries | The criteria only need an email. Storing PII brings retention and access obligations the PoC doesn't need. |
| Shared idempotency and rate-limit stores | In memory is correct for one instance. Redis or DynamoDB behind the same interfaces when scaling out. |
| HTML email | Plain text can't be abused for HTML injection and is enough for a sales inbox. |
| Auth and CORS | Public read endpoints plus a rate-limited enquiry endpoint. There's no browser client, so no CORS. |
| Offline-first | Needs a local DB as source of truth plus sync rules, which is well beyond the time-box, and stale prices are a risk. Enquiries need the network anyway. |
| Caching in the app | The backend caches the catalogue; the app doesn't need its own cache at ~20 products. See "Next". |
| Pagination | Fake Store has ~20 products. |
| Product create/update/delete | Admin features; this is a customer catalogue. |
| Encrypted storage | Nothing sensitive is stored: no auth, no tokens, and enquiries are sent rather than saved. |

## Security

- HTTPS only; cleartext allowed only for the local dev host (Android network security config, debug only).
- R8 minification in release.
- Input validated in the app and re-validated by the backend.
- No PII in logs; release builds log Warn and above.
- No secrets in the repo; config via `backend/.env.example` and Gradle properties.
- Idempotency key on enquiries so retries can't send duplicate emails.

## How it grows

| Change | Where it lands |
|---|---|
| New data source (real backend, different vendor) | A new data source in `core:data` plus one Koin binding. Domain and UI unchanged. |
| New look and feel | `core:ui` theme tokens only. |
| New email provider | A new `EmailSender` in the backend (SMTP relays such as SES need config only). The app sees `POST /enquiries`. |
| New product source behind the API | A new `ProductRepository` in `backend/src/catalogue/` and one line in `server.ts`. API and app unchanged. |
| Deploy to AWS | The backend image runs on ECS Fargate or App Runner behind an ALB (`TRUST_PROXY=true`), SES for email, Secrets Manager for SMTP credentials. |
| Caching | `CachingProductRepository` decorating the existing repository; one Koin binding. |
| Offline | A local data source behind `ProductRepository`; the repository decides between local and remote. |
| New feature (wishlist, cart) | A new `feature:*` module depending on `core:domain` and `core:ui`. |
| Breaking API change | `/api/v2`; old apps keep working on v1. |
| iOS app | See "Adding iOS" below. |

## Adding iOS

1. Add `iosArm64()` and `iosSimulatorArm64()` to each module, and a `binaries.framework` block in `:shared`.
2. Add the iOS `actual`s: `NSUUID` for `IdGenerator`, the Ktor Darwin engine.
3. Add a `MainViewController()` in `shared/iosMain` wrapping `App()`.
4. Create the Xcode project from the KMP wizard and point it at the `Shared` framework.

Domain, data, UI and the Compose UI tests carry over unchanged.

## Next, with another day

1. CI job that starts the backend, re-captures the contract fixtures and fails on any diff.
2. `CachingProductRepository` with a short TTL.
3. Pagination in the contract and grid.
4. Screenshot tests for light and dark themes.
5. Extend CI to the app (the backend already has it): build, run app tests, and app coverage (Kover once it supports
   AGP 9, or AGP's built-in JaCoCo).
6. Backend: generate an OpenAPI spec from the zod schemas, and store enquiries (DynamoDB) with an
   outbox so an SMTP outage delays emails instead of failing the request.
7. Home: a row per category under the top picks, like quick-commerce apps. First composed in the app from
   the existing endpoints; later a `GET /home` feed, so the backend decides the sections without an app release.

## Known limitations

Not fixed yet. Most were found in a review pass across the app; each has its fix.

- Two quick taps can navigate twice: two detail screens, two enquiry forms (each with its own idempotency
  key), or back past a screen. Fix: wrap navigation callbacks in `dropUnlessResumed`.
- An enquiry can get stuck. If the email is sent but the response is lost (timeout on a flaky network), the
  app shows an error; if the customer then edits the form and retries, the same key arrives with a different
  body, the backend answers `409`, and every retry fails the same way. Fix: a new key when the form is edited
  after a failure. Retrying an unchanged form keeps the key, so duplicates are still prevented.
- Leaving the form while it's sending (back, or tapping the product card) cancels the request, and the
  customer doesn't learn whether it went. Fix: block leaving for the short sending window.
- Pull-to-refresh while a category is still loading cancels that load; if the refresh then fails, the
  category shows a spinner that never ends. Fix: show the category error instead.
- A product that doesn't exist (`404`) still offers Retry, which can't help.
- The enquiry form isn't saved if Android kills the app in the background, so the customer retypes it.
  Fix: keep the form and its idempotency key in `SavedStateHandle`.
- The release build has no production backend URL; it's built in `scripts/ci.sh` only to catch R8 issues.
