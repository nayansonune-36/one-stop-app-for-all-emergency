#!/usr/bin/env bash
# ==============================================================================
# S.E.N.T.I.N.E.L. - make_base_india.sh
# Extracts a coarse, lightweight offline basemap for the whole of India (bbox 68,6,98,37, maxzoom 7).
# Keeps the file size under ~60MB as a fallback layer when offline outside the home zone.
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

# Public Protomaps build archive URL
# Protomaps hosts daily planet vector builds at https://build.protomaps.com/
# Example recent build URL:
BUILD_DATE="${BUILD_DATE:-20261108}"
SOURCE_URL="https://build.protomaps.com/${BUILD_DATE}.pmtiles"

INDIA_BBOX="68,6,98,37"
MAX_ZOOM="7"

echo "=== Extracting India Base PMTiles ==="
echo "Source: ${SOURCE_URL}"
echo "BBox:   ${INDIA_BBOX}"
echo "Zoom:   0 to ${MAX_ZOOM}"
echo "Target: ${OUTPUT_DIR}/india_base.pmtiles"

${PMTILES_BIN} extract "${SOURCE_URL}" "${OUTPUT_DIR}/india_base.pmtiles" \
    --bbox="${INDIA_BBOX}" \
    --maxzoom="${MAX_ZOOM}"

echo "Base India PMTiles extract complete."

# Convert to MBTiles for direct native SQLite reading if needed by MapLibre
echo "Converting PMTiles to MBTiles..."
${PMTILES_BIN} convert "${OUTPUT_DIR}/india_base.pmtiles" "${OUTPUT_DIR}/india_base.mbtiles"

echo "=== Finished successfully ==="
ls -lh "${OUTPUT_DIR}/india_base.pmtiles" "${OUTPUT_DIR}/india_base.mbtiles"
