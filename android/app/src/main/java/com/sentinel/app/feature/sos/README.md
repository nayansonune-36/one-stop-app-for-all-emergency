# S.E.N.T.I.N.E.L. Mesh SOS — Multi-Phone Testing Guide

This guide walks through testing offline mesh peer-to-peer distress propagation across 2 to 3 Android phones without internet connectivity.

---

## Architecture Summary
- **Protocol**: Google Nearby Connections API (`Strategy.P2P_CLUSTER`)
- **Fixed Service ID**: `com.sentinel.app.mesh`
- **Relay Policy**: Multi-hop relaying up to a maximum of 8 hops (`MAX_HOPS = 8`)
- **Deduplication**: Bounded cache of seen UUIDs (`MAX_SEEN_IDS = 1000`)
- **Gateway Sync**: Online nodes automatically forward packets to `BACKEND_BASE_URL/sos` via local retry queue

---

## Prerequisites
1. **2 or 3 Android devices** (Android 8.0 / API 26+ up to Android 14+).
2. Bluetooth and Location enabled in Android Quick Settings on all devices.
3. Backend server running (`cd backend && go run .`) on a machine reachable by at least one device (or emulator).

---

## Step-by-Step Test Procedure

### 1. Build and Install
Generate the APK:
```bash
cd android
./gradlew assembleDebug
```
Install `android/app/build/outputs/apk/debug/app-debug.apk` onto all phones:
```bash
./gradlew installDebug
```

### 2. Network Setup for 3-Phone Topology
- **Phone A (Distress Initiator)**: Turn **OFF** Wi-Fi and Mobile Data (100% offline).
- **Phone B (Mesh Relay)**: Turn **OFF** Wi-Fi and Mobile Data (100% offline).
- **Phone C (Internet Gateway)**: Turn **ON** Wi-Fi or Mobile Data with access to the backend IP.

*Physical placement:* Place Phone A near Phone B, and Phone B near Phone C (Phone A can be outside direct range of Phone C to verify multi-hop).

### 3. Grant Permissions
Open S.E.N.T.I.N.E.L. on all devices and grant:
- Nearby Devices / Bluetooth
- Location (GPS)

Each device immediately starts advertising and discovery under `com.sentinel.app.mesh` and automatically connects to peers.

### 4. Check Diagnostics Panel
1. Navigate to the **SOS** tab on any device.
2. **Long-press the screen title** (`Distress Beacon Broadcast`) to open the **Mesh Diagnostics Panel**.
3. Verify that nearby endpoints appear under **Active Connected Endpoints**.

### 5. Broadcast SOS on Phone A
1. On **Phone A**, press and hold the big red **EMERGENCY SOS** button for **3 seconds** until the progress ring fills (or tap and confirm).
2. Phone A attaches its GPS coordinates and broadcasts `SosMessage(hops=0)`.
3. The UI transitions to:
   - *"Looking for nearby devices"*
   - *"Sent to 1 device(s)"*

### 6. Verify Multi-Hop Relay on Phone B
1. **Phone B** receives the beacon from Phone A.
2. Phone B checks its seen cache, detects a new UUID, increments `hops` to `1`, and automatically relays the packet to Phone C.
3. Open the Diagnostics Panel on Phone B to see the logged event:
   ```
   [HH:mm:ss] Received new SOS <uuid> (hops: 0) from peer <endpointA>
   [HH:mm:ss] Relayed SOS <uuid> (hop count: 1) to 1 peer(s)
   ```

### 7. Verify Gateway Ingestion on Phone C
1. **Phone C** receives the relayed beacon (`hops=1`).
2. Because Phone C has active internet, its `ConnectivityObserver` triggers the gateway sync:
   - Queues SOS to `SosRetryQueue`.
   - Sends `HTTP POST /sos` to `BACKEND_BASE_URL/sos`.
3. Phone C status updates to: *"Delivered to emergency server"*.

### 8. Verify on Dispatch Dashboard
Open `http://localhost:8080/dashboard` (or `http://<backend-ip>:8080/dashboard`) in a browser:
- The incoming distress signal appears with:
  - Exact time
  - Latitude and Longitude
  - Device name (`SENTINEL-<model>`)
  - Medical summary
  - Hop counter (`1 hops`)
