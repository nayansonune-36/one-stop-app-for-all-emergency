# S.E.N.T.I.N.E.L. Offline Map Data & Tile Packaging Guide

This directory manages the offline emergency POI dataset and vector tile packages for S.E.N.T.I.N.E.L.

---

## 1. Offline Basemap Tiles

To comply with the strict **Zero-Network Offline-First** requirement, S.E.N.T.I.N.E.L. renders maps locally with **no remote HTTP tile requests**. 

### Bounding Box: Guwahati Metropolitan Region
- **Min Longitude**: `91.60`
- **Min Latitude**: `26.05`
- **Max Longitude**: `91.90`
- **Max Latitude**: `26.25`
- **Maximum File Size**: < 40 MB
- **Zoom Levels**: 10 to 15

### Generating MBTiles from OpenStreetMap
Follow these commands to generate a lightweight vector MBTiles package:

#### Prerequisites
- `curl`
- `osmium-tool` (optional, for clipping)
- [Planetiler](https://github.com/onthegomap/planetiler) or [Tilemaker](https://github.com/systemed/tilemaker)

#### Commands

```bash
# 1. Download regional Northeast India OSM extract
curl -fSL "https://download.geofabrik.de/asia/india/northeastern-states-latest.osm.pbf" -o northeast.osm.pbf

# 2. Extract Guwahati bounding box
osmium extract --bbox 91.60,26.05,91.90,26.25 northeast.osm.pbf -o guwahati.osm.pbf

# 3. Generate MBTiles using Planetiler
java -jar planetiler.jar \
  --osm-path=guwahati.osm.pbf \
  --mbtiles=guwahati.mbtiles \
  --bounds=91.60,26.05,91.90,26.25 \
  --min-zoom=10 \
  --max-zoom=14

# 4. Copy to Android assets
cp guwahati.mbtiles ../android/app/src/main/assets/map/guwahati.mbtiles
```

Or execute the automated helper script:
```bash
./data/tools/generate_tiles.sh
# or on Windows/Python:
python ./data/tools/generate_tiles.py
```

---

## 2. Bundled Offline Fallback Style

When an external MBTiles package is not bundled, the app loads `android/app/src/main/assets/map/offline_style.json` accompanied by `guwahati_basemap.geojson`. This renders:
1. High-contrast dark navy background (`#0B1220`).
2. Brahmaputra River natural waterway corridor polygon (`#1E2C4A`).
3. Guwahati municipal boundaries and major arterial transit corridors.
4. Categorized emergency pins for shelters, hospitals, and relief camps.

**Crucially, no remote style URLs, sprite URLs, or glyph URLs are contacted.** The map functions 100% reliably in Airplane Mode.

---

## 3. Emergency POI Registry (`shelters.json`)

`data/shelters.json` contains verified emergency facilities adhering to `docs/DATA.md`.
Each facility specifies:
- `id`: Unique identifier
- `name` / `name_hi`: English and Hindi names
- `type`: `"shelter"` | `"hospital"` | `"relief_camp"`
- `lat` / `lon`: Geographic coordinates
- `capacity`: Maximum occupancy capacity (or null)
- `address`: Physical address
- `phone`: Emergency telephone number (or null)
- `source` & `verified`: Audit provenance
