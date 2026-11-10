#!/usr/bin/env bash
# ==============================================================================
# S.E.N.T.I.N.E.L. - make_guwahati_demo.sh
# Extracts detailed offline basemap tiles for Guwahati (bbox 91.55,26.05,91.95,26.30, maxzoom 15).
# Used as the default prebuilt home pack for the demo build.
#
# Prerequisites:
#   Install pmtiles CLI:
#     - macOS: brew install pmtiles
#     - Linux/Go: go install github.com/protomaps/go-pmtiles/cmd/pmtiles@latest
#     - Or download release from: https://github.com/protomaps/go-pmtiles/releases
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/android/app/src/main/assets/maps"

mkdir -p "${OUTPUT_DIR}"

PMTILES_BIN="pmtiles"
if ! command -v "${PMTILES_BIN}" &> /dev/null; then
    echo "ERROR: 'pmtiles' CLI not found in PATH."
    echo "Please install it from https://github.com/protomaps/go-pmtiles/releases"
    echo "Or run: go install github.com/protomaps/go-pmtiles/cmd/pmtiles@latest"
    exit 1
fi

BUILD_DATE="${BUILD_DATE:-20261108}"
SOURCE_URL="https://build.protomaps.com/${BUILD_DATE}.pmtiles"

GUWAHATI_BBOX="91.55,26.05,91.95,26.30"
MAX_ZOOM="15"

echo "=== Extracting Guwahati Demo PMTiles ==="
echo "Source: ${SOURCE_URL}"
echo "BBox:   ${GUWAHATI_BBOX}"
echo "Zoom:   0 to ${MAX_ZOOM}"
echo "Target: ${OUTPUT_DIR}/guwahati.pmtiles"

${PMTILES_BIN} extract "${SOURCE_URL}" "${OUTPUT_DIR}/guwahati.pmtiles" \
    --bbox="${GUWAHATI_BBOX}" \
    --maxzoom="${MAX_ZOOM}"

echo "Guwahati Demo PMTiles extract complete."

# Convert to MBTiles for direct native SQLite reading if needed
echo "Converting PMTiles to MBTiles..."
${PMTILES_BIN} convert "${OUTPUT_DIR}/guwahati.pmtiles" "${OUTPUT_DIR}/guwahati.mbtiles"

echo "=== Finished successfully ==="
ls -lh "${OUTPUT_DIR}/guwahati.pmtiles" "${OUTPUT_DIR}/guwahati.mbtiles"
