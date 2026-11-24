# Officeworks catalogue PoC

A product catalogue where customers browse, search and enquire about products.
An Android app built with Kotlin Multiplatform and Compose Multiplatform, with light and dark themes,
backed by a Node.js + TypeScript API over the Fake Store API.

## What the app does

- **Home**: search bar, category chips (outlined icon; filled and highlighted when selected) and the
  5 featured products. Selecting a category filters the page in place: its 3 top-rated products, then
  the rest. Tapping the selected chip again returns to the featured products.
- **Search**: results for the typed term, or an empty state.
- **Product detail**: image, price badge, category, rating and description, with a pinned **Enquire** button.
- **Enquiry**: name and email (required), phone and message (optional), inline validation, then "Thank you".
  The sales inbox receives one email per enquiry, even if the customer retries.
- Every screen has loading, error (with retry) and empty states, in light and dark themes.

## Stack choice

- **Client: Android (Kotlin).** Code is structured as Kotlin Multiplatform with Compose Multiplatform UI,
  so an iOS app can be added later without a rewrite. There is no iOS target in this submission.
- **Backend: Node.js + TypeScript** (Fastify) in [`backend/`](backend/README.md). It implements
  [API_CONTRACT.md](API_CONTRACT.md) over the Fake Store API and emails enquiries over SMTP
  (Mailpit locally, so no accounts are needed). TypeScript because the brief prefers it; Fastify and
  zod give typed validation and fast in-process tests with little code.

Why: Kotlin and Compose are my strongest stack, and keeping domain, data and UI in shared Kotlin
modules enforces the boundaries the brief asks for. Full reasoning is in [DECISIONS.md](DECISIONS.md).

## Project structure

```
backend/             Node.js API (see backend/README.md)
docker-compose.yml   Backend + Mailpit (local mail catcher) in one command
scripts/             Contract fixture capture
androidApp/          Android entry point
shared/              App shell: navigation, DI wiring
core/domain/         Models, repository interfaces, use cases (pure Kotlin)
core/data/           API client, DTOs, mappers, repositories
core/ui/             Theme (light/dark) and shared components
feature/catalogue/   Home (with category filter), search, product detail
feature/enquiry/     Enquiry form and thank-you state
```

## Requirements

- JDK 17+
- Android Studio (latest stable) with an Android emulator
- For the backend, either Docker, or Node 22+ and [Mailpit](https://mailpit.axllent.org)

## Run

### 1. Backend

```bash
docker compose up --build
```

API on http://localhost:3000 (the emulator reaches it at `http://10.0.2.2:3000/api/v1/`).
Enquiry emails land in Mailpit at http://localhost:8025. More in [backend/README.md](backend/README.md).

Without Docker (macOS), use two terminal tabs and leave both running:

```bash
brew install mailpit && mailpit                                  # tab 1: mail catcher
cd backend && cp .env.example .env && npm install && npm run dev  # tab 2: API
```

Check it's up: `curl localhost:3000/api/v1/products/featured` returns 5 products.

### 2. Android app

The app has no offline or demo data: **start the backend first**, then:

```bash
./gradlew :androidApp:installDebug
```

Or select `androidApp` in Android Studio and run it on an emulator.

### 3. Walk the acceptance criteria

| Criterion | Where to see it |
|---|---|
| Home: 5 featured products | Home screen, in the order set by `FEATURED_PRODUCT_IDS` |
| Category: top rated, then the rest | Tap a category chip on Home |
| Search | Search bar, e.g. "jacket"; "zzzz" shows the empty state |
| Product page | Tap any product tile |
| Enquiry | Product page -> Enquire -> submit -> "Thank you"; the email appears at http://localhost:8025 |

## Configuration

| Gradle property | Default | Meaning |
|---|---|---|
| `officeworks.baseUrl` | `http://10.0.2.2:3000/api/v1/` | Backend base URL. `10.0.2.2` is the Mac as seen from the emulator. |

It's read into `BuildConfig.BASE_URL`. To point at another backend:

```bash
./gradlew :androidApp:installDebug -Pofficeworks.baseUrl=http://192.168.1.20:3000/api/v1/
```

Or put it in `~/.gradle/gradle.properties`. Debug builds allow plain HTTP to `10.0.2.2` and
`localhost` only; release builds are HTTPS-only.

## Checks and tests

### Full pipeline, one command

With an emulator running:

```bash
./scripts/ci.sh          # format check -> unit tests -> UI tests -> release build -> install the debug app
./scripts/ci.sh --fix    # same, but formats the code first instead of failing on style
```

It runs these steps in order and stops at the first failure, like a CI pipeline:

| Step | Gradle task | What it checks |
|---|---|---|
| 1 | `spotlessCheck` (or `spotlessApply` with `--fix`) | Formatting and lint: ktlint on every `.kt` and `.gradle.kts` file |
| 2 | `testAndroidHostTest --continue` | Unit tests in every module, on the JVM |
| 3 | `connectedAndroidDeviceTest` | Compose UI tests on the emulator, including navigation through the whole app |
| 4 | `:androidApp:assembleRelease` | Release build with R8, to catch shrinking problems. Built, not installed |
| 5 | `:androidApp:installDebug` | Builds the app and installs it on the emulator |

The script checks for a connected emulator before starting. The tests don't need the backend; the
installed app does, to show data.

### Individual steps

```bash
./gradlew spotlessApply                           # fix formatting
./gradlew testAndroidHostTest --continue          # app unit tests, every module (JVM, no emulator)
./gradlew connectedAndroidDeviceTest              # Compose UI tests, needs a running emulator
(cd backend && npm install && npm test)            # backend tests, no network needed
```

ktlint rules are in `.editorconfig`, so Android Studio formats the same way.

Each module writes an HTML report to `build/reports/tests/`. There is no coverage report for the app
yet; see DECISIONS.md.

`core:data` has contract tests that parse JSON captured from the real backend
(`core/data/src/commonTest/fixtures/`). After a backend change, re-capture with the backend running:

```bash
./scripts/capture-contract-fixtures.sh
```

## Docs

- [DECISIONS.md](DECISIONS.md): assumptions, cuts, how it grows, next steps
- [API_CONTRACT.md](API_CONTRACT.md): the backend contract the app is built against
- [AI_USAGE.md](AI_USAGE.md): how AI tools were used
- [CLAUDE.md](CLAUDE.md): rules for AI agents in this repo
