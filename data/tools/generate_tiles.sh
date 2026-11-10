#!/usr/bin/env bash
# S.E.N.T.I.N.E.L. Offline Tile Generation Script
# Bounding Box: Guwahati Region [minLon, minLat, maxLon, maxLat] -> [91.60, 26.05, 91.90, 26.25]
set -euo pipefail

OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PBF_FILE="${OUT_DIR}/guwahati_extract.osm.pbf"
MBTILES_OUT="${OUT_DIR}/guwahati.mbtiles"
ASSETS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../android/app/src/main/assets/map" && pwd)"

BBOX="91.60,26.05,91.90,26.25"
MIN_ZOOM=10
MAX_ZOOM=15

echo "=========================================================="
echo " S.E.N.T.I.N.E.L. - Offline Basemap Tile Package Generator"
echo " Target Bounding Box: ${BBOX}"
echo " Zoom Levels: ${MIN_ZOOM} - ${MAX_ZOOM}"
echo "=========================================================="

# Step 1: Download OSM extract via Overpass API / Geofabrik / BBBike
if [ ! -f "${PBF_FILE}" ]; then
    echo "Downloading OSM PBF extract for Guwahati region..."
    curl -fSL "https://overpass-api.de/api/map?bbox=${BBOX}" -o "${OUT_DIR}/guwahati.osm"
    echo "Converting raw OSM to PBF using osmium-tool..."
    if command -v osmium &> /dev/null; then
        osmium cat "${OUT_DIR}/guwahati.osm" -o "${PBF_FILE}"
        rm -f "${OUT_DIR}/guwahati.osm"
    else
        echo "Note: osmium-tool not found. Renaming raw XML to osm for tilemaker processing."
        mv "${OUT_DIR}/guwahati.osm" "${OUT_DIR}/guwahati.osm"
    fi
fi

# Step 2: Generate Vector MBTiles using Tilemaker or Planetiler
if command -v tilemaker &> /dev/null; then
    echo "Generating MBTiles using Tilemaker..."
    tilemaker --input "${PBF_FILE}" \
              --output "${MBTILES_OUT}" \
              --bbox "${BBOX}" \
              --minzoom "${MIN_ZOOM}" \
              --maxzoom "${MAX_ZOOM}"
elif command -v planetiler &> /dev/null; then
    echo "Generating MBTiles using Planetiler..."
    planetiler --osm-path="${PBF_FILE}" \
               --mbtiles="${MBTILES_OUT}" \
               --bounds="${BBOX}" \
               --min-zoom="${MIN_ZOOM}" \
               --max-zoom="${MAX_ZOOM}"
else
    echo "WARNING: Neither 'tilemaker' nor 'planetiler' is installed."
    echo "To install Tilemaker: https://github.com/systemed/tilemaker"
    echo "To install Planetiler: https://github.com/onthegomap/planetiler"
    echo "Alternatively, download pre-rendered tiles using python script: python generate_tiles.py"
    exit 1
fi

# Step 3: Bundle into Android Assets
echo "Copying ${MBTILES_OUT} to ${ASSETS_DIR}..."
mkdir -p "${ASSETS_DIR}"
cp "${MBTILES_OUT}" "${ASSETS_DIR}/guwahati.mbtiles"

echo "Offline basemap tile bundle successfully created and bundled!"
