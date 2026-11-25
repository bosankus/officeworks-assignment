# AI usage

## Tools and models

- **Claude (Opus 5.5)** in the Claude desktop app, with the project folder connected. Two sessions: one for
  the Android app and one for the backend, both working from [CLAUDE.md](CLAUDE.md) and
  [API_CONTRACT.md](API_CONTRACT.md).
- The AI wrote the code and docs. I ran every Gradle build, unit test run, UI test run and emulator check on
  my Mac and pasted the output back: the AI's sandbox couldn't reach the Gradle repositories, the emulator
  or the running backend.
- Claude's browser pane, to read the Fake Store docs and check the live API and the Mailpit inbox.
- No other AI tools.

## Agent instructions

[CLAUDE.md](CLAUDE.md) is the only agent instruction file. It was written before any code: module and
dependency rules, architecture, conventions, logging, tests, and a "Don't" list. When a mistake showed a
rule was missing, the rule was added (typographic quotes in strings, the direct resources dependency, running
`testAndroidHostTest` rather than `allTests`), so the same mistake doesn't come back.

## Plan, as executed

1. Read the brief and confirm the platform choice and constraints.
2. Agree architecture, modules, patterns and libraries (stable versions only) before any code.
3. Docs first: CLAUDE.md, API_CONTRACT.md, DECISIONS.md, README and this file.
4. Gradle modules and dependencies; I synced and reviewed before any code was written.
5. App code layer by layer, domain -> data -> UI, with tests at each layer.
6. Backend built to API_CONTRACT.md in a separate session.
7. Switch the app to the backend, remove all demo data, capture contract fixtures from the running backend.
8. Walk the app on the emulator; fix what that showed.
9. Polish from using the app (pull-to-refresh, smaller chips, an enquiry redesign), each change its own commit.
10. A senior review pass across the app for leaks, races, error handling and recomposition; what it found is
    in DECISIONS.md, "Known limitations".
11. Finalise the docs.

## Key moments

1. **Reading the brief before building.** I asked whether the challenge was Android-first. It isn't: one
   client plus one backend, and the client must not call Fake Store directly. So API_CONTRACT.md was written
   first, and the backend and app were each built and tested against it.
2. **Disagreeing in both directions.** The AI proposed skipping use cases for pass-through reads; I overruled
   it so every change has an obvious home. I asked for data -> domain -> UI; it pushed back that data
   implements domain's interfaces, so domain comes first, and I agreed. I suggested an interface for every
   use case; it argued that seven single-implementation interfaces add files without making anything
   swappable, and I agreed again (DECISIONS.md).
3. **Dropping the iOS target.** We started with Android and iOS through Compose Multiplatform. Re-reading the
   brief, its iOS option is Swift or Objective-C, so a Kotlin iOS target could be marked down. iOS was
   removed and the KMP layout kept, so it can be added back (DECISIONS.md, "Adding iOS").
4. **Testing against the real backend.** I had all demo data removed from the app, overriding a task brief
   that kept it as an option, so the app can only run against the backend. The `core:data` tests parse JSON
   captured from the running backend, so a change on either side breaks a test, not the app.
5. **Using and reviewing the app instead of adding features.** On the emulator, a separate category screen
   lost the chips and search, so categories now filter Home in place. Then I asked for a lead-developer review
   of the whole codebase. It found real bugs (the worst, an enquiry that can get stuck in a `409` loop after a
   lost response), API details leaking into the domain, and duplicated error mapping. The design issues were
   fixed; the remaining bugs are written up with fixes in DECISIONS.md, "Known limitations". I also had
   Compose-specific lint rules added to CI, which found a `LaunchedEffect` that could call a stale callback.

## How the output was checked

- Every build and test run was mine, on my Mac; the AI fixed from the pasted output.
- Unit tests in every module with logic, including contract tests on captured backend responses
  (`./gradlew testAndroidHostTest`).
- Compose UI tests for every acceptance criterion, plus navigation tests through the whole app, run on
  API 31 and API 36 (Android 16) emulators.
- The acceptance criteria walked by hand on the emulator against the running backend.
- Backend: `npm run check` (typecheck, type-aware lint, tests, coverage floor) and a CI smoke test that runs
  `docker compose up` and checks exactly one email lands in Mailpit for a retried enquiry.
- Backend failure paths on a real machine: with Mailpit stopped an enquiry returned `502`; after restarting
  it, the retry with the same key produced exactly one email.

## Where the AI got it wrong

**Approach**

- It recommended against a logging library, then reversed once I asked for structured logging for API
  calls. Kermit was the right call.
- It picked Kover 0.9.1 for coverage without being able to check that it supports AGP 9's KMP library
  plugin, and flagged that as a risk. It didn't support it, and the first sync failed. Kover was removed, so
  the app has no coverage number and none is claimed.
- It planned Compose UI tests in `commonTest`, then realised they'd also run in Android host tests on the
  plain JVM, where they can't render. Moved to `androidDeviceTest`.
- It told me to run `./gradlew allTests`. With only an Android target, that compiles the tests but doesn't
  run them, so a green build had run nothing new. I noticed the test reports were older than the build; the
  right task is `testAndroidHostTest`.

**Code**

- It used Ktor's `appendPathSegments` for the category slug, which doesn't encode `/`, so a slug like
  `../admin` could change the request path. It caught this while writing the test and switched to
  `encodeURLPathPart()`.
- It wrote `\"` in a string resource, which rendered literally. A UI test caught it.
- In the redesign it replaced the home screen's `Scaffold` with a `Column` plus a background colour. That
  doesn't set the text colour, so in dark theme unstyled text fell back to black on a dark background. I
  spotted it on the emulator; the dark-theme UI test only checked that text was displayed, which can't catch
  colour. Fixed with a `Surface` on the home screen and one at the app root.
- In a review pass I asked for, it found its own design flaws: the API's field names and error codes had
  leaked into the domain (`SubmitEnquiryUseCase` matched on `"name"` and `"REQUIRED"`), the error-to-message
  mapping was copied in two features, and prices hardcoded `$` for AUD. All three were moved to where they
  belong (data layer, `core:ui`, a locale-aware formatter).
- The contract tests read fixture files inside the mock HTTP engine, so a missing file was swallowed and
  reported as a confusing `ClassCastException`. The reads were moved out so a missing fixture fails clearly.
- After giving server errors their own message, it left one ViewModel test expecting the old generic one.
  The test run caught it.
- It committed imports out of ktlint's order in the home screen, which would have failed `spotlessCheck`.
  It noticed while editing the next screen and checked every file it had touched.
- It imported `assertDoesNotExist`, which is a member function, the same mistake as `onNode` earlier. It
  caught it re-reading the change, before a build.
- It made the thank-you screen scrollable, which stops its content from centring. Caught by re-reading.

**Build**

- Compose only generates a module's `Res` class when `components-resources` is a direct dependency. It hit
  this in `:shared`, fixed only that module, and then hit it again in both feature modules.
- When it moved the UI tests, it didn't add `kotlin-test` to `androidDeviceTest` (device tests don't inherit
  `commonTest`'s dependencies), and it added an import for `onNode`, which is a member function. Both were
  compile errors.
- The UI tests passed on an API 31 emulator but all failed on API 36: Compose UI test brings in an older
  Espresso that calls a removed Android method. Declaring Espresso 3.7.0 directly fixed it.
- It added the Compose lint rules without checking which ktlint they're built against. They need ktlint
  1.8.0, the project pinned 1.7.1, and the mix crashed a rule on every file. It had tested with a standalone
  ktlint, which hid the clash; the first CI run showed it.

**Process**

- It committed with `git add` on whole files without checking the diff, so edits made to those files
  between its commits went in under its own commit messages. One of them undid "clear the Home search field
  after searching". A UI test caught it; the AI restored the behaviour and now checks diffs before committing.
- A search-field UI test failed twice and it misdiagnosed it both times (first "the icon loads late"). The
  failure message already said why: the decorative icon is merged into the text field's accessibility
  node, so it only exists in the unmerged tree.
- It assumed the JVM's currency formatter sets the right decimals for a currency, as Android's does. The
  yen test failed (`¥1,200.00`); the decimals are now set explicitly.

**Backend**

- Search first used substring matching, so "mens cotton" returned a women's T-shirt ("mens" inside "womens").
  A test caught it; search now matches the start of words.
- A failing email sender returned `500` instead of `502`, because only the SMTP adapter wrapped its errors.
  Fixed in the service, so any email adapter behaves the same.
- While wiring the logger, it briefly left a placeholder `server.ts` (`undefined as never` for the services).
  It was replaced before anything ran, but it's the kind of stub to look for in AI output.
- Its first catalogue cache blocked on a refresh once the TTL expired. While Fake Store was down, every
  request would wait out the 5-second timeout before getting stale data, and each one hit Fake Store again.
  It found this in its own review pass a day later; a failing test came first, then the switch to
  stale-while-revalidate.
- In that same review, its first bounded idempotency store trimmed for size before looking up the key, so a
  retry at the size limit could evict its own entry and send a second email. It caught this by tracing
  the new test by hand before running it.
- It picked TypeScript 7 without checking tooling support. Adding ESLint showed typescript-eslint stops
  at 6.0, so it was pinned to 6.0.
- Fake Store was unreachable from its sandbox, so the backend's test fixture was first written from the
  documented response shape. The live API showed every image URL had changed (`.jpg` -> `t.png`) and that the
  docs omit `rating`, which the API does return. The fixture was corrected.

## Things the AI caught

- Turning on the Compose lint rules found a `LaunchedEffect` that read a callback directly, so it could call
  a stale one. It now reads it through `rememberUpdatedState`.

- The project template pinned Material3 to an alpha; it proposed the stable version.
- MockK is JVM-only, so mocks in shared tests would break any non-JVM target. Shared tests use hand-written
  fakes; MockK is only used in Android host tests.
- Fake Store has no featured list, no search, no enquiry endpoint, raw category names, and returns `200` for
  unknown ids. Those gaps shaped API_CONTRACT.md.
- The backend's `409 IDEMPOTENCY_KEY_REUSED` wasn't in the contract or handled by the app. Both were fixed.
- A commit message said ESLint was set up, but `backend/eslint.config.js` was never committed, so lint and CI
  would have failed on a fresh clone. It found the untracked file when splitting changes into commits.

## Time spent

Over the 2–3 hour time-box. The extra time went on the backend, a design pass after using the app on the
emulator, and two review passes. Figures are rounded estimates from the session history.

| Phase | Time |
|---|---|
| Planning (brief, architecture, API contract, docs first) | 1.5 h |
| Building (app ~4.5 h, backend ~1.5 h) | 6 h |
| Testing (unit and UI tests, emulator runs, fixing failures) | 2.5 h |
| Reviewing (code review passes, lint rules, docs, PR) | 2 h |
| **Total** | **12 h** |
