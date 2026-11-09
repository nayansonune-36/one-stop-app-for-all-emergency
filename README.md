# S.E.N.T.I.N.E.L.
**Smart Emergency Network & Triage INtelligence Engine Link**

S.E.N.T.I.N.E.L. is an offline-first disaster resilience and emergency triage ecosystem designed to save lives during natural disasters, infrastructure collapse, and connectivity blackouts.

---

## Key Capabilities

- **Offline-First Resilience:** Zero cloud dependencies for core emergency features including local SOS signaling, medical ID access, shelter lookup, and first aid guidance.
- **Mesh-Ready SOS Signal:** Standardized SOS packets with UUID deduplication and hop tracking.
- **Dispatch Dashboard:** Live-updating web monitoring console for rescue coordinators.
- **Bilingual Experience:** Complete English and Hindi localization with instant in-app switching without restarting device.
- **AI Triage Assistant:** Online-capable conversational triage engine for dynamic first-aid questions.

---

## Repository Structure

```
├── android/          # Android client (Kotlin, Jetpack Compose, Material 3, ViewModel + StateFlow)
├── backend/          # Ingestion & coordination backend (Go 1.22+, standard net/http)
├── docs/             # Technical specifications (API.md, DATA.md, ARCHITECTURE.md)
├── data/             # Offline POI and shelter registry (shelters.json)
├── AGENTS.md         # Multi-agent roles, rules, and code conventions
└── README.md         # Repository documentation
```

---

## Quickstart

### 1. Backend Service (Go)

Prerequisites: Go 1.22+

```bash
cd backend
go run .
```
The server will listen on `http://localhost:8080`.
- Health check: `http://localhost:8080/health`
- Live Dashboard: `http://localhost:8080/dashboard`

Environment Variables:
- `PORT`: Service port (default `8080`)

### 2. Android Application

Prerequisites: Android SDK 34, JDK 17+ or 21+

```bash
cd android
./gradlew assembleDebug
```
The debug APK is produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

To configure backend endpoint:
Create or edit `android/local.properties`:
```properties
BACKEND_BASE_URL=http://10.0.2.2:8080
```
(Default is `http://10.0.2.2:8080` for Android emulator).

---

## Architecture & API Docs

- [System Architecture](file:///docs/ARCHITECTURE.md)
- [REST API Specification](file:///docs/API.md)
- [Deployment Guide](file:///docs/DEPLOY.md)
- [Data Schemas & POI Registry](file:///docs/DATA.md)
- [First Aid Clinical Statements & Medical Review](file:///android/app/src/main/java/com/sentinel/app/feature/firstaid/README.md)
- [Contributor & Agent Guidelines](file:///AGENTS.md)
