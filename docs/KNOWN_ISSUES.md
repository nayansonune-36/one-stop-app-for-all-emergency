# S.E.N.T.I.N.E.L. — Known Issues & Technical Observations

This document records architectural decisions, device-specific behaviors, and observations resulting from the UI v2 design system merge.

---

## 1. Design Reference Merge Decisions

The design reference (`docs/design/ui-v2/`) from Figma Make provided visual layouts, typography, and color tokens. In accordance with the project requirements, all simulated and prototype data was replaced with real production implementations:

| Feature Area | Prototype Behavior | Production Android Implementation |
|---|---|---|
| **Theme & Typography** | CSS variables (`:root` / `.dark`) | Jetpack Compose `MaterialTheme` with dynamic light/dark mode, Devanagari support, 48dp touch targets, and two-line chip labels. |
| **Shelters / Map** | Hardcoded fake shelters ("Harbor Ave") | Real offline `assets/shelters.json` loaded via `kotlinx.serialization`, rendered on MapLibre Native offline vector basemap. |
| **SOS Beacon** | Canned animation and fake stages | Real Google Nearby Connections (`P2P_CLUSTER`), bounded seen-ID cache (8 hops max), real FusedLocationProvider GPS, and automatic gateway retry queue. |
| **First Aid Guides** | 7 generic guide cards | 10 comprehensive offline emergency topics stored in `assets/firstaid/firstaid_en.json` & `firstaid_hi.json` with step-by-step instructions. |
| **AI Triage Bot** | Canned responses & mock latency | Live HTTP client calling `BACKEND_BASE_URL/chat` with offline detection, First Aid quick redirect, and persistent 112/108 emergency disclaimer. |
| **Medical ID & QR** | Generic mock QR graphic & localStorage | SharedPreferences encrypted persistence, flat JSON format matching `QrScanner.tsx`, offline ZXing bitmap generation, and CameraX + ML Kit analyzer. |
| **Security & Locking** | Static mock PIN lock | AndroidX `BiometricPrompt` supporting biometric authentication (Fingerprint/Face) and system device credential fallback (PIN/pattern). |
| **Onboarding** | Browser permissions query API | Real Android runtime permission launchers (Location, Camera, Microphone, Notifications, Bluetooth/Nearby) with system settings intent deep-links. |

---

## 2. Hardware & Device Considerations

### 2.1 Camera & Torch Support on Emulators
- **Symptom:** In Android Studio emulators without an emulated flash unit, toggling the flashlight in `QrScanScreen` has no effect.
- **Handling:** `QrScanScreen` inspects `camera.cameraInfo.hasFlashUnit()`. If no flash hardware is present, the torch button is disabled with an in-context string indicator (`R.string.qr_torch_off`).

### 2.2 Biometric Prompt & Device Credentials
- **Symptom:** On devices running Android 10 (API 29) or below without enrolled biometrics, invoking `setAllowedAuthenticators` with both `BIOMETRIC_STRONG` and `DEVICE_CREDENTIAL` can trigger an exception on older vendor OEM ROMs.
- **Handling:** `BiometricHelper` branches conditionally based on `Build.VERSION.SDK_INT`:
  - API 30+: uses `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`.
  - API 26–29: uses `setDeviceCredentialAllowed(true)`.
  - If no device credential is set on the device, user can unlock using an in-app 4-digit PIN configured in Settings.

### 2.3 Nearby Connections Strategy & Permissions
- **Android 12+ (API 31+)**: Requires runtime `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, and `BLUETOOTH_CONNECT`.
- **Android 13+ (API 33+)**: Requires `NEARBY_WIFI_DEVICES` with `usesPermissionFlags="neverForLocation"`.
- **Android 11 and below (API ≤ 30)**: Requires `ACCESS_FINE_LOCATION` to discover nearby Bluetooth LE endpoints.
- **Handling:** `MeshPermissionHelper` abstracts these requirements per API level, ensuring the app requests only the permissions necessary for that Android version.

---

## 3. Localization Verification Status

- Full parity verified with `docs/tools/check_strings.py`:
  - **291** unique user-facing keys defined in `res/values/strings.xml`.
  - **291** identical matching keys translated in `res/values-hi/strings.xml`.
  - **232** distinct `R.string.*` references in Kotlin code all cleanly resolve.

---

## 4. Hybrid Map Architecture & Offline Vector Tile Packaging

### 4.1 Zero-OpenStreetMap Foundation Tile Policy
- S.E.N.T.I.N.E.L. does **not** fetch tiles from `tile.openstreetmap.org` under any condition, adhering strictly to OSMF tile usage guidelines.
- Live tiles are routed through `TILES_BASE_URL` (configurable to self-hosted PMTiles on Cloudflare R2 / AWS S3 or commercial vector tile providers like MapTiler).

### 4.2 LocalTileServer & MapLibre Native Compatibility
- MapLibre Native 11.5.0 operates with standard vector tile URLs (`{z}/{x}/{y}.pbf`) and SQLite MBTiles.
- An embedded loopback tile server (`LocalTileServer` on `127.0.0.1`) mediates between MapLibre and local tile archives (`.pmtiles` and `.mbtiles`).
- Tile source priority order:
  1. `HOME_PACK` (Guwahati demo or downloaded Home Zone bounding box, zoom 0–15).
  2. `LIVE_SOURCE` (online stream from `TILES_BASE_URL`).
  3. `INDIA_BASE` (bundled base map for India, zoom 0–7).
  4. `OVERZOOM_OR_EMPTY` (HTTP 204 No Content for clean overzoom without visual artifacts).

### 4.3 Build Environment Network Isolation
- Generating fresh PMTiles archives (`india_base.pmtiles` and `guwahati.pmtiles`) requires network access and the `pmtiles` CLI tool.
- Automated extraction scripts are provided in `data/tools/make_base_india.sh` (and `.bat`) and `data/tools/make_guwahati_demo.sh` (and `.bat`).
- Once generated, tile archives placed in `android/app/src/main/assets/maps/` are automatically ingested into internal storage on launch.

  - No hardcoded English or Hindi strings in Compose UI components.

---

## 4. Shelter Dataset & Map Considerations

- **Sample Data Scope:** The `/data/shelters.json` dataset represents sample/demo data spanning Assam (13), Odisha (4), Kerala (4), Bihar (3), Tamil Nadu (2), and Maharashtra (1). It is strictly sample data for simulation and demo purposes and must never be represented or badged as live, complete, or verified in the UI.
- **Approximate Coordinates:** 13 entries have `coord_precision: "approximate"`, representing town or neighborhood centroids rather than exact building locations. Five entries share identical coordinate pairs. In the UI, overlapping entries are clustered with count badges, and approximate entries display an `"Approximate location"` badge. Numeric distance calculations and nearest-shelter sorting strictly exclude approximate entries to prevent misleading navigation cues.
- **50 km Nearest-Shelter Threshold:** The nearest emergency shelter logic evaluates only exact entries within a 50 km radius of the user. If no exact shelters qualify within 50 km, the app displays `"No shelter found near you yet"`.
- **District Control Room Numbers:** Several phone numbers in the registry route to district disaster management authority (DDMA) or municipal emergency control rooms (e.g. 1077, 1913, DDMA numbers) rather than direct on-site facility phone desks. These are labeled as `"District control room"` in the client detail sheet.
- **Machine-Generated Hindi Names:** 10 shelter entries have machine-written Hindi translations (`name_hi`) flagged for future human review by native translators.
- **Offline Basemap Coverage:** High-detail offline vector street tiles (`guwahati_basemap.geojson` / MBTiles) cover the Guwahati metropolitan region only. For other states/regions (Odisha, Kerala, Bihar, Tamil Nadu, Maharashtra), the app renders points on the dark offline background with an informative note (`"Detailed offline map available for Guwahati only"`), maintaining 100% offline isolation without fetching remote online tiles.

---

## 5. Medical ID QR & Offline Detection Hardening

- **Empty Profile QR Guard:** Previously, tapping "Show Emergency QR" on a fresh install generated an empty JSON payload (`{"v":1}`) which contained zero medical data and was rejected as invalid upon scanning. Tapping the button now validates whether any medical fields exist, shows an informative notice to fill in and save details first, and provides an in-modal decoded summary card under the QR graphic.
- **Scanner Decoding Resiliency:** In `QrScanScreen`, ML Kit is configured with `BarcodeScannerOptions(FORMAT_QR_CODE)` and 720p analysis resolution. It extracts `rawValue ?: displayValue` and includes a ZXing `PlanarYUVLuminanceSource` fallback for frames where ML Kit might not resolve low-contrast captures.
- **Airplane Mode Instant Offline Transition:** Previously, querying `checkInitialConnectivity()` inside `onLost()` during active Airplane Mode transitions could query `activeNetwork` before the OS finished tearing down the radio interfaces, leaving the status badge stuck in "ONLINE". The observer now binds `registerDefaultNetworkCallback`, inspects `Settings.Global.AIRPLANE_MODE_ON`, and listens to `Intent.ACTION_AIRPLANE_MODE_CHANGED` for instantaneous offline UI updates.

---

## 6. Map Gestures, Blank Basemap & Location Puck Resolution

- **Root Cause 1 — MapView Lifecycle & Frozen Touch Gestures:** In Jetpack Compose, the `MapView` was initialized inside `AndroidView.factory` with only `onCreate(null)`, but `onStart()` and `onResume()` were omitted. Because the parent activity was already in `RESUMED` state when navigating to the Map tab, the `LifecycleEventObserver` never received `ON_START` or `ON_RESUME` transitions. Consequently, MapLibre's native render thread and gesture detector (`StandardGestureDetector`, pan, pinch, scroll) remained permanently paused, preventing the user from moving or exploring the map.
  - **Fix:** The `MapView` is now managed via a dedicated `remember` block that explicitly calls `onCreate(null)`, `onStart()`, and `onResume()` upon instantiation, followed by binding to the lifecycle for subsequent background/foreground events. All gestures are also explicitly enabled in `map.uiSettings`.
- **Root Cause 2 — Style Loading via Inlined GeoJSON vs File URI Parsing:** Loading styles via `file:///` URLs on Android internal storage produced path formatting edge cases (e.g. four slashes `file:////data/...` resulting from concatenating `file:///` with Linux leading `/`), which caused native C++ URI loaders to fail silently. When the style failed to load, the `map.setStyle` callback never fired, leaving `maplibreMapInstance` null and rendering controls unresponsive.
  - **Fix:** `MapAssetManager` now constructs the style JSON with **inlined GeoJSON data** and delivers it via `Style.Builder().fromJson(mapStyleJson)`. This loads 100% in-memory without any file URI resolution or native asset streaming, ensuring guaranteed offline style compilation.
- **Root Cause 3 — Basemap Extent Beyond Guwahati:** The original basemap GeoJSON contained only a 10 km urban polygon around Guwahati. When the user was located anywhere else in India, or panned away from Guwahati, the screen was an empty dark blue `#0B1220` void with zero geographic context.
  - **Fix:** Bundled `assets/map/india_basemap.geojson` containing the Republic of India landmass boundary, the 6 emergency response state zones (Assam, Bihar, Kerala, Maharashtra, Odisha, Tamil Nadu), major river systems (Brahmaputra, Ganges, Godavari, Krishna), and the primary National Highway trunk corridors (NH 27, NH 44, NH 16, NH 48), plus full Guwahati urban detail.
- **Root Cause 4 — Camera & User Location Centering:** The camera was hardcoded to center on Guwahati `LatLng(26.15, 91.75)`, placing the user's location puck hundreds of kilometers off-screen.
  - **Fix:** Added initial camera auto-centering to the user's GPS coordinates if already acquired, high-contrast cyan/blue pulsing user location puck, and hardened the "My Location" FAB with safe `animateCamera` / `moveCamera` fallbacks.

---

## 7. Backend Issues

### 7.1 Upstream LLM Provider Authentication Failure (`POST /chat`)
- **Origin:** Go Backend (`/backend/main.go`, `chat_service.go`, `llm_provider.go`)
- **Description:** When the backend server is launched without a valid `LLM_API_KEY` environment variable (or with a test placeholder), calls to the triage assistant fail during upstream LLM invocation.
- **Exact HTTP Request:**
  ```http
  POST /chat HTTP/1.1
  Host: 10.0.2.2:8080
  Content-Type: application/json

  {
    "lang": "en",
    "messages": [
      {
        "role": "user",
        "content": "Severe bleeding on arm"
      }
    ]
  }
  ```
- **Exact HTTP Response:**
  ```http
  HTTP/1.1 502 Bad Gateway
  Content-Type: application/json
  Access-Control-Allow-Origin: *

  {
    "error": "The first-aid triage assistant is temporarily unavailable. If this is an emergency, please call 112 or 108 immediately."
  }
  ```
- **Backend Error Log:**
  ```
  [Chat ERROR] Upstream LLM call failed for client ::1: upstream llm returned status code 401
  [POST] /chat [::1]:52086 - 502 (713.0073ms)
  ```
- **Client Handling:** The Android app does not crash or display raw JSON errors. `BotRepository` extracts the backend `error` message and `BotViewModel` displays the polite guidance notice along with the persistent emergency Call 112/108 button.

---

## 8. Demo Stability Fixes Applied

1. **Cleartext HTTP Permission (`AndroidManifest.xml`):**
   - Added `android:usesCleartextTraffic="true"` to `<application>`. Fixes `IOException: CLEARTEXT communication to 10.0.2.2 not permitted by network security policy` on Android 9+ (API 28–34) when syncing SOS alerts or chat requests to the local backend.
2. **Wi-Fi Permissions for Mesh Clustering (`AndroidManifest.xml`):**
   - Added `<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />` and `<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />`. Prevents Google Nearby Connections from failing with `8033: MISSING_PERMISSION_CHANGE_WIFI_STATE` during P2P_CLUSTER advertising and discovery.
3. **Basemap Switch to OpenFreeMap & Graceful Fallback (`MapScreen.kt`, `MapViewModel.kt`):**
   - Configured vector basemap to use OpenFreeMap (`https://tiles.openfreemap.org/styles/liberty`, no API key required).
   - Displayed required attribution banner: `OpenFreeMap | © OpenStreetMap contributors`.
   - Camera defaults to Guwahati `(26.1445, 91.7362)` at zoom 12.0 when GPS is unavailable or outside shelter regions; never zooms out to all-India overview.
   - Removed unbundled "Guwahati only" offline card. Added map loading failure listener that falls back to inlined local style or dark background so the screen never hangs.
4. **SOS Auto-Trigger on Confirmation & Navigation Flow (`HomeScreen.kt`, `SentinelNavHost.kt`, `SosScreen.kt`):**
   - Passing `autoTrigger=true` route argument when confirming SOS on Home or tapping "Send SOS" in Bot critical banner, immediately transitioning to the Active Distress Dashboard without requiring a duplicate 3-second hold.
   - Added an in-app Back button in the top header of `SosScreen` to return to Home cleanly.
5. **SOS State & Delivery Reset (`MeshManager.kt`, `SosViewModel.kt`):**
   - Added `resetDeliveryState()` to reset `isDeliveredToServer` and `sentDeviceCount` when a new SOS broadcast starts or when an existing broadcast is cancelled.
6. **Robust Error Extraction in Triage Bot (`BotRepository.kt`, `BotViewModel.kt`):**
   - Catches non-200 HTTP responses and parses backend error JSON (`{"error": "..."}`) for display instead of throwing unhandled exceptions.
7. **Orientation Change Protection (`AndroidManifest.xml`):**
   - Configured `android:configChanges="locale|layoutDirection|orientation|screenSize|screenLayout|keyboardHidden"` to prevent activity recreation and state resets on device rotation.
8. **100% Localization Parity & Zero-Width Scan:**
   - 336 unique user-facing keys in both `strings.xml` and `strings-hi.xml` (0 missing, 0 extra).
   - Python Unicode scan verified 0 invisible zero-width or formatting characters in `strings-hi.xml`.

---

## 9. Known Prototype Limitations for Demo (Ranked by Likelihood)

| Rank | Issue / Limitation | Likelihood in Demo | Impact & Graceful Behavior |
|---|---|---|---|
| **1** | **Triage Bot Upstream LLM Offline / Key Missing** | **High** (if no live Gemini API key is configured in backend environment) | Returns HTTP 502/503. Client gracefully shows: `"The first-aid triage assistant is temporarily unavailable. If this is an emergency, please call 112 or 108 immediately."` No crash, no frozen UI. |
| **2** | **Nearby Mesh Peer Count on Single Emulator** | **Medium** (if demo uses a single emulator instance) | Peer count displays `0`. The alert immediately syncs to the backend gateway over local HTTP (`10.0.2.2:8080/sos`), showing status `"Delivered to emergency server"`. |
| **3** | **Emulator Camera Preview for QR Scanner** | **Medium** (if emulator lacks virtual camera input) | Camera permission prompt appears. If denied, shows clean permission rationale with `"Allow camera"` button; back button returns safely to Medical ID. |
| **4** | **Emulator Default GPS in California** | **Low** (standard Android emulator behavior) | Because California coordinates `(37.42, -122.08)` are outside India shelter regions, map auto-centers on Guwahati at zoom 12.0 and displays `"No shelter found near you yet"`. |
