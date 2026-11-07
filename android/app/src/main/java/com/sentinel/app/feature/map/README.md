# S.E.N.T.I.N.E.L. Offline Emergency Map (`feature/map`)

## Overview

The Map feature provides a **100% offline-first emergency facility locator and navigation viewer** designed to operate without cellular connectivity or internet access during disaster scenarios.

### Core Architecture
- **Rendering Engine**: MapLibre Native Android (`org.maplibre.gl:android-sdk`).
- **Offline Basemap Style**: `asset://map/offline_style.json` — dark high-contrast vector style matching S.E.N.T.I.N.E.L. design system (`#0B1220`, `#121A2B`, `#182C48`) with local water corridors (Brahmaputra River) and regional municipal boundaries (`asset://map/guwahati_basemap.geojson`).
- **MBTiles Bundling Support**: `MapAssetManager` automatically detects and extracts any pre-bundled vector MBTiles tile packages (`guwahati.mbtiles` under 40 MB) from assets to internal storage on launch.
- **Offline POI Registry**: Parses verified shelters, hospitals, and relief camps from `assets/shelters.json` using `kotlinx.serialization` (as specified in `docs/DATA.md`). Gracefully handles empty registry (`[]`) without crashing.
- **Dynamic Proximity Triage**: Calculates real-time Haversine great-circle distance between the user's GPS fix and nearby facilities, dynamically updating the **Nearest Emergency Facility** card.
- **Localization**: Full dual English and Hindi (Devanagari) localization for all strings, names, and categories.

---

## Airplane Mode Verification Guide

Follow these steps to verify zero-network offline functionality on a physical Android device or emulator:

### Step 1: Enable Airplane Mode
1. Swipe down the Android Notification Shade / Quick Settings.
2. Tap **Airplane Mode** to disable Cellular Data, Wi-Fi, and Internet connectivity.
3. Confirm that no active network connection is present (status bar shows airplane icon).

### Step 2: Launch App & Open Map Tab
1. Open S.E.N.T.I.N.E.L.
2. Tap the **Map** tab on the bottom navigation bar.
3. **Verify**:
   - The offline dark basemap renders immediately with zero network errors.
   - The top offline badge displays: `Offline Basemap (Guwahati Bounding Box)`.
   - No crash occurs, and Logcat confirms no external HTTP network requests are initiated.

### Step 3: Verify Marker Display & Category Legend
1. Observe the emergency facility markers rendered across the Guwahati region:
   - **Hospital (Red Circle with White Cross)**: e.g., Gauhati Medical College & Hospital (GMCH).
   - **Relief Camp (Cyan Circle with White Tent)**: e.g., Sarusajai Indoor Stadium Relief Center.
   - **Shelter (Amber Circle with White House)**: e.g., Nehru Stadium Emergency Shelter.
2. If location permission is granted, observe the green pulsing user location dot marker at your GPS position.

### Step 4: Verify Nearest Shelter Card & Distance
1. At the top of the screen, locate the **Nearest Emergency Facility** card.
2. Check that it displays:
   - Facility name in the active app language (English or Hindi).
   - Calculated Haversine distance (e.g., `1.8 km away`).
3. Tap the card: the map camera animates and zooms directly to the facility, opening its detail sheet.

### Step 5: Verify Facility Detail Bottom Sheet
1. Tap any facility marker on the map.
2. Verify that the bottom sheet displays:
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
