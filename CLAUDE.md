# CLAUDE.md

Rules for AI agents working in this repo. Read this before writing any code.

## What this is

A product catalogue PoC (browse, search, enquire). An Android app, structured as Kotlin
Multiplatform with Compose Multiplatform UI so iOS can be added later. Android is the only target.

It must grow past the PoC: the data source, look and feel, and email provider are all expected to change.

## Modules and dependency rules

```
androidApp          ->  shared
shared              ->  feature:*, core:*        (app shell: navigation, Koin graph)
feature:catalogue   ->  core:domain, core:ui
feature:enquiry     ->  core:domain, core:ui
core:data           ->  core:domain
core:ui             ->  core:domain              (only for DataError -> UiError)
core:domain         ->  (coroutines only)
```

- Features never depend on `core:data` or on each other.
- Only `shared` knows concrete implementations; it binds them in Koin.
- Cross-feature navigation lives in `shared`. Features expose callbacks (`onEnquire(productId)`).
- Package root: `com.bosankus.officeworks.<module>`.
- Shared code goes in `commonMain`. Platform code only through `expect`/`actual`, with the
  `actual` in `androidMain`. Never put logic in `androidMain` that iOS would also need.

## Architecture

Clean Architecture + MVVM with unidirectional data flow.

- **Domain**: models, repository interfaces, use cases. Pure Kotlin.
- **Data**: DTOs, mappers, data sources, repository implementations.
- **Presentation**: one ViewModel per screen, one immutable `UiState`, sealed `Event`s via `onEvent()`.
  ViewModels expose `val uiState: StateFlow<UiState>` (backed by `_uiState`) and depend **only on use cases**.

## Conventions

- Every path goes through a use case: one class, one `operator fun invoke`.
- Use cases depend only on repository interfaces.
- Results: `DataResult<T>` (`Success` / `Failure(DataError)`). Do not use `kotlin.Result` or `runCatching`
  in suspend code; always rethrow `CancellationException`.
- Ids are value classes: `ProductId`, `CategorySlug`.
- Money is `Long` cents in the domain. Convert from the DTO `Double` in the mapper only.
- Mappers are extension functions (`ProductDto.toDomain()`), not interfaces.
- UI state is a sealed interface; screens render with an exhaustive `when`.
- Use cases and repositories are main-safe (Ktor suspends, nothing blocks). ViewModels use `viewModelScope`;
  tests swap `Dispatchers.Main` for a test dispatcher.
- Strings live in Compose resources. No hardcoded user-facing text, including formats such as
  `4.7 | 120`: use a resource with placeholders (`%1$s | %2$s`). Use typographic quotes and
  apostrophes (“ ” ’) rather than escaped ones.
- A module with Compose resources declares `compose.components.resources` directly and sets
  `generateResClass = always`, or its `Res` class won't be generated.
- Colours, type and shapes come from `AppTheme` in `core:ui`. No raw colour values in features.
- No raw `dp` outside `core/ui/.../theme`: padding and gaps use `Spacing`, component sizes use `Sizes`,
  corner radii use `MaterialTheme.shapes`. Add a named value to `Dimensions.kt` rather than a one-off number.

## Don't

- No `BaseViewModel`, no generic `UseCase<P, R>` base class, no mapper interfaces, and no interface per use
  case. Abstractions go where implementations change (data sources, repositories); see DECISIONS.md.
- No new library without a reason written in DECISIONS.md. Stable versions only (no alpha, beta, RC).
- No calls to fakestoreapi.com from the app (images excepted, see DECISIONS.md). Product data comes
  only through our backend (see API_CONTRACT.md). No demo or bundled data in production code.
- No PII in logs (name, email, phone, message). Log product ids and error types only.
- No logging in composables. No request or response bodies in logs.

## Screens

Each screen has a public `XxxScreen` (gets its ViewModel from Koin, collects state) and an internal,
stateless `XxxContent(state, onEvent, callbacks)` that tests and previews use.

## Compose performance

- UI models and UI states are `@Immutable` data classes, so unchanged items skip recomposition.
- Every lazy list/grid item has a stable `key` and a `contentType`.
- Read state as low as possible: screens pass plain values down, not the ViewModel.
- No mapping, sorting or formatting inside composables; ViewModels hand over display-ready values.
- Decorative drawing uses `Canvas`/`drawBehind`, not extra layout nodes.

## Logging (Kermit)

| What | Level | Where |
|---|---|---|
| API call: method, path, status, duration | Debug | `ApiCallLogger` Ktor plugin |
| API or parsing failure: endpoint, `DataError`, cause | Warn / Error | `safeApiCall` |
| Enquiry submitted / failed (product id only) | Info / Error | Repository |
| Unexpected error | Error | ViewModel |

Release builds log Warn and above.

## KDoc

One line on public interfaces and use cases. Say *why*, not *what*. No `@param` unless the
name isn't obvious. Don't comment self-explanatory code.

## Tests

- `commonTest`: hand-written fakes, `kotlin-test`, `kotlinx-coroutines-test`, Turbine.
  Covers use cases, validators, mappers, ViewModel state, and the backend contract tests, which parse
  JSON captured from the real backend (`core/data/src/commonTest/fixtures/`).
- Run them with `./gradlew testAndroidHostTest`. `allTests` compiles but does not run Android host tests.
- Android host tests: MockK for interaction checks only.
- Compose UI tests: `androidx.compose.ui.test.v2.runComposeUiTest` in feature `androidDeviceTest`, against
  the stateless `XxxContent` composables. Cover every acceptance criterion, plus dark theme rendering.
  Test names use underscores (DEX doesn't allow spaces in method names). Device tests don't include
  `commonTest`, so they declare their own `kotlin-test-junit` and `espresso-core`.
- Every use case, mapper, repository, error mapping and ViewModel has unit tests. There's no coverage
  tool for the app yet (see DECISIONS.md), so don't quote a percentage.
- Test names: `` `returns top rated first when category has more than three products`() ``.

## Before handing work back

Run `./scripts/ci.sh` (format check, unit tests, UI tests, release build, install). Fix style with
`./gradlew spotlessApply`; don't hand-format or disable ktlint rules inline.
Compose rules run in the same check: give every public composable that emits UI a `modifier: Modifier = Modifier`
parameter, and read callbacks inside effects through `rememberUpdatedState`. A rule is suppressed only with a
comment saying why.

## Build order

Domain -> data -> UI. Don't start a layer until the previous one compiles and its tests pass.

## Backend (`backend/`)

- Node 22, TypeScript strict, ESM. Fastify, zod, nodemailer, Vitest. No new dependency without a reason in DECISIONS.md.
- Layers: `routes.ts` (HTTP and validation) -> services -> ports (`ProductRepository`, `EmailSender`,
  `IdempotencyGuard`). Only `server.ts` chooses adapters.
- Services never import Fastify. Vendor DTOs never leave their adapter folder (`catalogue/fakestore/`).
- Throw `AppError` subclasses; `http/errorHandler.ts` turns them into the contract's error shape. Never build error replies by hand.
- API changes start in API_CONTRACT.md, then a failing test, then code.
- No PII in logs (name, email, phone, message): ids and error types only.
- Tests use `app.inject` with fixtures and fakes. No network in tests.
- `npm run check` (typecheck, lint, tests, coverage floor) must pass before backend work is called done.
- TypeScript stays on 6.0 until typescript-eslint supports 7.
