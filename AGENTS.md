# AGENTS.md — Contributor & Multi-Agent Collaboration Guide

## 1. Project Summary
**S.E.N.T.I.N.E.L.** (*Smart Emergency Network & Triage INtelligence Engine Link*) is an offline-first disaster response and emergency assistance monorepo. It comprises:
- An offline-first Android application (`/android`) providing localized emergency SOS signaling, triage bot, offline maps/shelter discovery, medical ID, and first aid guides.
- A lightweight Go backend (`/backend`) providing emergency ingestion, mesh-deduplicated SOS storage, incident dispatch dashboard, and AI bot stubs.
- Documentation (`/docs`) and offline data registry (`/data`).

> [!IMPORTANT]
> **Offline-First Requirement:** Everything in the application MUST work completely offline with zero internet access, with the sole exception of the AI triage chatbot (`feature/bot`), which gracefully requires internet connectivity.

---

## 2. Repository Layout
```
.
├── .gitignore               # Unified Android and Go ignore rules
├── README.md                # General project overview and quickstart
├── AGENTS.md                # Agent assignments, conventions, and rules
├── data/
│   └── shelters.json        # Verified offline POI registry (shelters, relief camps, hospitals)
├── docs/
│   ├── API.md               # Backend REST API definitions and SOS schemas
│   ├── DATA.md              # Shelter dataset JSON format specification
│   └── ARCHITECTURE.md      # System topology, Android layers, and backend architecture
├── backend/
│   ├── go.mod               # Go module definition (Go 1.22+)
│   └── main.go              # Standard net/http server, SOS store, dashboard, chat stub
└── android/
    ├── gradlew / gradlew.bat
    ├── build.gradle.kts     # Root build file
    ├── settings.gradle.kts  # Project settings
    └── app/
        ├── build.gradle.kts # Single-module app config (minSdk 26, targetSdk 34)
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/sentinel/app/
            │   ├── MainActivity.kt
            │   ├── core/
            │   │   ├── medical/      # MedicalSummaryProvider interface & null default
            │   │   └── network/      # ConnectivityObserver (StateFlow<Boolean>)
            │   ├── feature/
            │   │   ├── home/         # Dashboard, large SOS button, status chip, shortcuts
            │   │   ├── map/          # Offline shelters map placeholder
            │   │   ├── medical/      # Medical ID placeholder
            │   │   ├── bot/          # AI triage bot placeholder (requires internet)
            │   │   ├── sos/          # SOS emergency broadcast placeholder
            │   │   ├── firstaid/     # First aid guide cards placeholder
            │   │   ├── settings/     # In-app language switch & settings placeholder
            │   │   └── onboarding/   # Language choice & permission explainer placeholder
            │   ├── navigation/       # SentinelNavHost, BottomNavigation, routes
            │   └── ui/theme/         # Theme.kt, Color.kt, Type.kt, Spacing.kt
            └── res/
                ├── values/strings.xml     # English string resources
                └── values-hi/strings.xml  # Hindi string resources
```

---

## 3. Navigation Architecture (UI v2 Decision)

The bottom navigation bar contains 5 primary tabs:
1. **Home (`/home`)**: Central command screen containing time-of-day greeting, online/offline status chip, large 3-second hold-to-activate SOS beacon button, Essential Tools shortcut grid, and First-Aid Essentials quick row.
2. **Map (`/map`)**: Offline MapLibre Native basemap with local vector tile packages and verified shelters/hospitals registry.
3. **First Aid (`/firstaid`)**: Offline emergency action protocols (10 topics) with instant search and step-by-step CPR/trauma instructions.
4. **Bot (`/bot`)**: AI Triage assistant with critical situation detection and Call 112/108 banner (gracefully requires internet).
5. **Settings (`/settings`)**: In-app language switcher (English / हिन्दी), notification preferences, Medical ID security lock, and system permission overview.

**Secondary Screen Flows:**
- **Medical ID (`/medical`)**: Emergency profile accessible via the Home "Essential tools" grid and Settings.
- **SOS Detail View (`/sos`)**: Active distress dashboard displaying live mesh relay stages, peer count, hops, location, and diagnostics panel, which opens automatically upon activating SOS from Home or navigating directly.
- **Onboarding (`/onboarding`)**: First-run permission and setup flow.

---

## 4. Ownership Matrix

| Area / Package | Responsible Agent / Owner |
|---|---|
| `android/app/.../feature/map` | **Lead** |
| `android/app/.../feature/sos` & mesh networking | **Lead** |
| `/backend` (Go API, store, dashboard) | **Lead** |
| Shell, root repo files, architecture & app navigation | **Lead** |
| `android/app/.../feature/medical` | **Dev A** |
| `android/app/.../feature/firstaid` | **Dev A** |
| `android/app/.../feature/bot` | **Dev B** |
| `android/app/.../feature/settings` | **Dev B** |
| Localization (`res/values/strings.xml`, `res/values-hi/strings.xml`) & i18n audit | **Dev B** |
| `/data/shelters.json` dataset curation & verification | **Dev C** |

> [!WARNING]
> **Strict Area Boundary Rule:** Do NOT edit files outside your own assigned area without explicit authorization from the **Lead**.

---

## 4. Coding Conventions

- **Kotlin Official Style Guide:** Adhere strictly to official Kotlin styling, naming conventions, and idiomatic Kotlin practices.
- **Single ViewModel per Screen:** Each interactive screen must be backed by exactly one `ViewModel` exposing a single immutable `StateFlow` representation of UI state.
- **Zero Hard-Coded Strings:** Every user-visible string must be defined in both `res/values/strings.xml` and `res/values-hi/strings.xml`, and accessed using `stringResource(R.string.<id>)`.
- **Zero Business Logic in Composables:** Composable functions must only emit UI declarations and forward events (callbacks) to the `ViewModel`. State manipulation and side effects belong in ViewModels and core domain providers.
- **No DI Frameworks:** Keep the Android layer lightweight; use plain constructor injection and straightforward factory/provider singletons.
- **Go Standard Library:** Use Go 1.22+ standard library (`net/http`, `sync`, `encoding/json`). Do not introduce third-party web frameworks unless explicitly approved.

---

## 5. Branching and Pull Request Rules

1. **Branch Naming:**
   - Features: `feature/<agent-or-dev>/<short-desc>` (e.g., `feature/dev-a/medical-id`)
   - Fixes: `fix/<issue-desc>`
2. **Commit Messages:**
   - Follow Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`.
3. **Verification Before PR:**
   - Android: `./gradlew assembleDebug` must compile and pass without errors.
   - Backend: `go build ./...` must compile cleanly without errors.
4. **Code Review:**
   - Any PR touching shared contracts (`core/`, `navigation/`, `docs/`) requires explicit Lead approval.

---

## 6. How to Build and Run

### Android App
```bash
cd android
./gradlew assembleDebug
```
The output APK is generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

To install and run on a connected device / emulator:
```bash
./gradlew installDebug
```

### Go Backend
```bash
cd backend
go run .
```
Or build the binary:
```bash
cd backend
go build -o bin/sentinel-backend .
./bin/sentinel-backend
```
The backend starts on port `:8080` (or `PORT` env var).
- Health check: `http://localhost:8080/health`
- Incident Dashboard: `http://localhost:8080/dashboard`
