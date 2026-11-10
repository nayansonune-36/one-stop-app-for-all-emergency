# S.E.N.T.I.N.E.L. Hybrid All-India Emergency Map (`feature/map`)

## Overview

The Map feature provides a **hybrid offline-first emergency facility locator and navigation viewer** designed to operate seamlessly across all of India both online and offline during disaster scenarios:

### Core Architecture & Hybrid Map Strategy
- **Home Zone (Local Pack)**: The user's home area (25–50 km radius) is stored directly on the device and always works offline up to zoom 15. When viewport intersects the home zone, local tiles are used even when online.
- **Rest of India (Online Live Vector Tiles)**: Live map tiles when connected to the internet via `TILES_BASE_URL` (configured via `local.properties` or environment).
- **Rest of India (Offline Fallback)**: Coarse low-zoom base map bundled with the app (`india_base.pmtiles`/`india_base.mbtiles` up to zoom 7) accompanied by a floating card *"Detailed map unavailable offline here"*.
- **Rendering Engine**: MapLibre Native Android (`org.maplibre.gl:android-sdk`) powered by an internal loopback tile server (`LocalTileServer` on `127.0.0.1`) that dynamically negotiates source order.
- **Source Order**:
  1. `HOME_PACK`: Inside user's configured Home Zone bounding box (online or offline).
  2. `LIVE_SOURCE`: Outside Home Zone when device has active network connectivity.
  3. `INDIA_BASE`: Outside Home Zone when offline for zoom <= 7.
  4. `OVERZOOM_OR_EMPTY`: Returns HTTP 204 No Content for clean overzoom without visual artifacts or blank screen.
- **Ambient Cache**: 100 MB limit set on MapLibre Native ambient cache via `OfflineManager.setMaximumAmbientCacheSize`.
- **Zero OpenStreetMap Abuse**: Never fetches from `tile.openstreetmap.org`.

---

## Emulator & Physical Device Manual Testing Procedure

### Step 1: Set Emulator GPS to Guwahati
1. In Android Studio Emulator Extended Controls (`...` button):
   - Navigate to **Location**.
   - Enter Latitude: `26.1445`, Longitude: `91.7362` (Guwahati, Assam).
   - Click **Save Point** and **Set Location**.

### Step 2: Set Home Zone to Guwahati
1. Launch S.E.N.T.I.N.E.L.
2. In Onboarding or under **Settings > Offline maps**:
   - Verify Home Zone is set to **Guwahati** (prefilled from location or selected via city search).
   - Check status chip displays: `Home area: offline ready` (or wait for download worker to complete).
   - Observe pack size and internal storage allocation.

### Step 3: Test Home Zone in Airplane Mode (Offline)
1. Turn ON **Airplane Mode** on the device / emulator.
2. Open the **Map** tab:
   - Verify status chip displays: `Home area: offline ready`.
   - Pan and zoom in to zoom levels 14 to 16 around **GMCH (Gauhati Medical College)** and **Dispur**.
   - Verify streets, buildings, and local labels render crisp without network connectivity.

### Step 4: Pan to Delhi in Airplane Mode (Offline Fallback)
1. Still in **Airplane Mode**, pan the map across India to **Delhi** (Lat `28.6139`, Lon `77.2090`).
2. Verify:
   - Coarse India base map renders at regional zoom levels (zoom <= 7).
   - Status chip indicates: `Offline: limited detail here`.
   - The floating warning card **"Detailed map unavailable offline here"** appears at the bottom.
   - Tapping the card opens **Offline maps** settings to configure or download that zone.

### Step 5: Disable Airplane Mode (Online Outside Home Zone)
1. Turn OFF **Airplane Mode** to re-enable Wi-Fi or cellular data.
2. Return to the Map screen viewing Delhi:
   - Notice the status chip transitions to `Online map`.
   - Detailed live vector tiles stream and render dynamically for Delhi and surrounding regions.
   - The "Detailed map unavailable offline here" card automatically dismisses.

### Step 6: Verify Markers, Clustering & Nearest Facility Rules
1. Markers are clustered when facilities share coordinates.
2. Tapping a cluster expands the list sheet.
3. Approximate markers display dashed yellow borders and the `"Approximate location"` badge.
4. Nearest shelter card respects the strict 50 km and `coord_precision == "exact"` rules. If no exact facility is within 50 km, it shows `"No shelter found near you yet"`.
   - Facility title in selected language (`name` or `name_hi`).
   - Category badge with matching color and icon.
   - Street address.
   - Maximum capacity (or "Capacity: Unknown").
   - Verification date audit stamp.
   - **Call Facility** action button (tapping launches `Intent.ACTION_DIAL` with the emergency number).
   - **Focus on Map** button to re-center the map view.

### Step 6: Verify Filter Chips
1. Tap the filter chips below the top card:
   - Tap **Shelter**: only amber shelter markers remain visible; the nearest shelter recalculates for shelters only.
   - Tap **Hospital**: only red hospital markers remain visible.
   - Tap **Relief Camp**: only cyan relief camp markers remain visible.
   - Tap **All**: all markers are restored.

### Step 7: Verify Empty Registry Graceful Handling
1. Replace `assets/shelters.json` with an empty array `[]`.
2. Re-launch the app.
3. Verify that instead of crashing, a clean empty state card appears: *"No Verified Shelters Found — The offline shelter registry is currently unpopulated."*
