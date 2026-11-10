# S.E.N.T.I.N.E.L. Architecture

**Smart Emergency Network & Triage INtelligence Engine Link**

---

## 1. System Overview

S.E.N.T.I.N.E.L. is an offline-first disaster response and emergency assistance ecosystem designed to function reliably in crisis scenarios where cellular networks, electricity, and centralized cloud infrastructures are severed or overloaded.

```
+-------------------------------------------------------------------------------+
|                             S.E.N.T.I.N.E.L. SYSTEM                          |
+-------------------------------------------------------------------------------+

[ Citizen Device A ] (Offline)
       |
       | Bluetooth LE / Wi-Fi Direct Mesh
       v
[ Citizen Device B ] (Offline Relay)
       |
       | Bluetooth LE / Wi-Fi Direct Mesh
       v
[ Gateway Node C ] (Cellular / Wi-Fi Restored)
       |
       | HTTP POST /sos (Deduplicated via UUID)
       v
+-----------------------------+
|        Backend Server       |  <--- Polls every 3s ---  [ Dispatch Dashboard ]
|          (Go :8080)         |                           (Browser UI)
+-----------------------------+
       ^
       | HTTP POST /chat
       v
[ AI Triage Bot ] (Online-only)
```

---

## 2. Android Architecture (`/android`)

The Android application follows standard modern Android architecture principles without unnecessary framework bloat:

- **Single Module:** `app` (`com.sentinel.app`)
- **UI Framework:** 100% Jetpack Compose with Material 3
- **Design System:** High-contrast dark emergency theme (`ui/theme`)
  - Primary Red (`#E53935`) for SOS / critical actions
  - Dark Navy Background (`#0B1220`)
  - Navy Surface (`#121A2B`)
  - High-visibility White text (`#FFFFFF`)
  - Typography rendering Latin and Devanagari script (Noto Sans font family)
- **State Management:** Android Architecture `ViewModel` + Kotlin `StateFlow`
- **Dependency Injection:** Lightweight manual injection / service locators (No Hilt/Dagger/Koin overhead)
- **Navigation:** Jetpack Navigation Compose with a single `MainActivity` host
- **Localization:** System and in-app dynamic language switching (`en` and `hi`) using `AppCompatDelegate.setApplicationLocales`

### Package Structure
```
com.sentinel.app/
├── core/
│   ├── network/
│   │   ├── ConnectivityObserver.kt    # StateFlow<Boolean> for online/offline detection
│   │   └── NetworkConnectivityObserver.kt
│   └── medical/
│       ├── MedicalSummary.kt          # Blood group, allergies, conditions, contact
│       └── MedicalSummaryProvider.kt  # Interface with null default implementation
├── feature/
│   ├── home/         # Dashboard with big SOS trigger, status chip, navigation shortcuts
│   ├── map/          # Offline shelters and emergency POI map
│   ├── medical/      # Medical ID personal card and emergency contact management
│   ├── bot/          # AI triage assistant (online-only requirement)
│   ├── sos/          # SOS broadcast & status screen
│   ├── firstaid/     # Offline first-aid guidance cards
│   ├── settings/     # App settings & dynamic language switch
│   └── onboarding/   # Permissions explainer and language selection
├── navigation/       # SentinelNavHost, NavRoutes, BottomBar
└── ui/
    └── theme/        # Color.kt, Type.kt, Theme.kt, Spacing.kt
```

---

## 3. Core Principles & Offline Strategy

1. **Strict Offline First:**
   All features except the online AI chatbot (`feature/bot`) function with zero internet connectivity:
   - Emergency SOS beacon generation and local storage
   - Shelter / hospital lookup using `/data/shelters.json`
   - Medical ID access for first responders
   - Offline first-aid card guides
2. **Network Resilience & Deduplication:**
   When connectivity is detected via `ConnectivityObserver`, pending distress signals are synced to the backend. Each SOS packet carries a unique UUID and a hop counter to prevent broadcast storms and duplicate entries.
3. **Decoupled Feature Compilation:**
   The `MedicalSummaryProvider` interface allows the `feature/sos` module to compile and operate independently of `feature/medical`. When `feature/medical` is merged, it satisfies the provider contract.

---

## 4. Backend Architecture (`/backend`)

The backend is built in standard Go (Go 1.22+) using standard library packages (`net/http`, `sync`, `encoding/json`):

- **Concurrency-Safe In-Memory Store:**
  - `sync.RWMutex` protecting in-memory map of SOS events keyed by `id`
  - Array sorted newest first on retrieval
- **Middleware:**
  - Structured request logging middleware (method, path, latency, status)
  - Cross-Origin Resource Sharing (CORS) handler for web dashboards and mobile clients
- **Real-Time Dispatch Dashboard:**
  - Embedded HTML/CSS/JavaScript console (`GET /dashboard`)
  - Auto-polls `GET /sos` every 3 seconds to update emergency table
- **Configuration:**
  - Environment variables (e.g. `PORT=8080`, default `8080`)
