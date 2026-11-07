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
  - **271** unique user-facing keys defined in `res/values/strings.xml`.
  - **271** identical matching keys translated in `res/values-hi/strings.xml`.
  - **231** distinct `R.string.*` references in Kotlin code all cleanly resolve.
  - No hardcoded English or Hindi strings in Compose UI components.
