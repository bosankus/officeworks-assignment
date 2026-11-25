# Officeworks catalogue PoC

An Android app for browsing, searching and enquiring about products, with a small backend in front of
the [Fake Store API](https://fakestoreapi.com). Enquiries are emailed to a sales inbox.

Kotlin Multiplatform + Compose Multiplatform (Android target only for now), and a Node.js + TypeScript
backend. Why: [DECISIONS.md](DECISIONS.md).

## Run it

You need **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** and
**[Android Studio](https://developer.android.com/studio)**. Works on macOS, Windows and Linux.

**1. Start the backend** (from the project folder; leave it running)

```bash
docker compose up --build
```

Ready when http://localhost:3000/api/v1/products/featured shows 5 products.

**2. Open the project in Android Studio and start an emulator**

Opening it once lets Android Studio find your Android SDK. Then **Device Manager** → ▶ on any phone.

**3. Install the app**

| macOS / Linux | Windows |
|---|---|
| `./gradlew :androidApp:installDebug` | `gradlew.bat :androidApp:installDebug` |

Or in Android Studio, pick **androidApp** and press ▶ Run. If the command line says Java is missing,
use the Run button, or point `JAVA_HOME` at the JDK bundled with Android Studio.

**4. Try it**

| Check | How |
|---|---|
| 5 featured products | Open the app |
| Category: 3 top rated, then the rest | Tap a category chip; tap it again to go back |
| Search | Type "jacket" and press search; "zzzz" shows the empty state |
| Product page | Tap any product |
| Enquiry | Product → **Enquire** → fill in name and email → **Send** → "Thank you". The email arrives at http://localhost:8025 |

<details>
<summary>No Docker? Run the backend with Node 22+ and Mailpit instead</summary>

Install [Mailpit](https://mailpit.axllent.org/docs/install/) (catches the enquiry emails):

| macOS | Linux | Windows |
|---|---|---|
| `brew install mailpit` | `sudo sh < <(curl -sL https://raw.githubusercontent.com/axllent/mailpit/develop/install.sh)` | Download from [Mailpit releases](https://github.com/axllent/mailpit/releases) |

Then, in two terminals:

```bash
mailpit                                 # terminal 1
cd backend && npm install && npm run dev  # terminal 2
```

On Windows PowerShell 5, run the second line as `cd backend`, `npm install`, `npm run dev`.
Every backend setting has a working default; `backend/.env.example` lists them.

</details>

<details>
<summary>Real phone or another machine?</summary>

The emulator reaches your computer at `10.0.2.2`. For anything else, pass the backend's address:

```bash
./gradlew :androidApp:installDebug -Pofficeworks.baseUrl=http://192.168.1.20:3000/api/v1/
```

</details>

## How it fits together

```mermaid
flowchart LR
    subgraph App["Android app · Kotlin Multiplatform + Compose"]
        direction LR
        UI["Screens<br/>Home · Search · Detail · Enquiry"] --> VM["ViewModels<br/>one UiState each"]
        VM --> UC["Use cases<br/>core:domain"]
        UC --> Repo["Repositories + Ktor client<br/>core:data"]
    end
    Repo -- "REST API (API_CONTRACT.md)" --> API["Backend<br/>Node.js + Fastify"]
    API -- "catalogue, cached 5 min" --> FS["Fake Store API"]
    API -- "enquiry email, SMTP" --> Mail["Sales inbox<br/>Mailpit locally"]
    UI -. "product images only" .-> FS
```

The app talks only to the backend. Each enquiry carries an idempotency key, so a retry can't email
sales twice.

## Run the checks

One command runs everything in order, like CI: format and lint → unit tests → UI tests → release build →
install. It needs an emulator running, not the backend.

```bash
./scripts/ci.sh          # add --fix to auto-format first
```

On Windows, run it from Git Bash or WSL. Backend checks: `cd backend && npm run check` (also run by
GitHub Actions on every PR that touches `backend/`).

| Step | What it checks |
|---|---|
| 1 | ktlint and Compose rules on every Kotlin file |
| 2 | Unit tests in every module (JVM) |
| 3 | Compose UI tests and whole-app navigation tests on the emulator |
| 4 | Release build with R8 (built, not installed) |
| 5 | Installs the debug app |

After a backend change, refresh the app's contract test data (backend running):
`./scripts/capture-contract-fixtures.sh`.

## Project layout

```
backend/            Node.js + TypeScript API (backend/README.md)
androidApp/         Android entry point
shared/             App shell: navigation and dependency wiring
core/domain/        Models, repository interfaces, use cases (pure Kotlin)
core/data/          API client, DTOs, repositories
core/ui/            Theme (light/dark) and shared components
feature/catalogue/  Home, category filter, search, product page
feature/enquiry/    Enquiry form and thank-you screen
```

## More

- [DECISIONS.md](DECISIONS.md): why it's built this way, assumptions, what was cut, known limitations
- [API_CONTRACT.md](API_CONTRACT.md): the API both sides are built against
- [AI_USAGE.md](AI_USAGE.md): how AI was used, and where it got things wrong
- [CLAUDE.md](CLAUDE.md): rules for AI agents working in this repo
