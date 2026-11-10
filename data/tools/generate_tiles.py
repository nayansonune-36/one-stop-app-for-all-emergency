#!/usr/bin/env python3
"""
S.E.N.T.I.N.E.L. Offline Tile Generator Script
Bounding Box: Guwahati [minLon: 91.60, minLat: 26.05, maxLon: 91.90, maxLat: 26.25]
Generates or downloads offline vector/raster MBTiles package under 40 MB.
"""
import sys
import os
import shutil
import urllib.request

BBOX = {
    "min_lon": 91.60,
    "min_lat": 26.05,
    "max_lon": 91.90,
    "max_lat": 26.25
}

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.abspath(os.path.join(script_dir, ".."))
    assets_dir = os.path.abspath(os.path.join(script_dir, "../../android/app/src/main/assets/map"))
    os.makedirs(assets_dir, exist_ok=True)

    print("==========================================================")
    print(" S.E.N.T.I.N.E.L. Offline Basemap Generation Tool")
    print(f" Target Region: Guwahati, Assam ({BBOX['min_lat']}, {BBOX['min_lon']} to {BBOX['max_lat']}, {BBOX['max_lon']})")
    print("==========================================================")
    
    mbtiles_path = os.path.join(data_dir, "guwahati.mbtiles")
    target_asset = os.path.join(assets_dir, "guwahati.mbtiles")

    if os.path.exists(mbtiles_path):
        size_mb = os.path.getsize(mbtiles_path) / (1024 * 1024)
        print(f"Found existing MBTiles file ({size_mb:.2f} MB): {mbtiles_path}")
        shutil.copy2(mbtiles_path, target_asset)
        print(f"Copied to Android assets: {target_asset}")
        return

    print("MBTiles tile bundle not found locally.")
    print("To generate high-resolution offline vector tiles:")
    print("1. Download regional OSM extract:")
    print("   curl -fSL 'https://download.geofabrik.de/asia/india/northeastern-states-latest.osm.pbf' -o northeast.osm.pbf")
    print("2. Clip to Guwahati bounding box using osmium:")
    print(f"   osmium extract --bbox {BBOX['min_lon']},{BBOX['min_lat']},{BBOX['max_lon']},{BBOX['max_lat']} northeast.osm.pbf -o guwahati.osm.pbf")
    print("3. Generate vector MBTiles using Planetiler or Tilemaker:")
    print("   planetiler --osm-path=guwahati.osm.pbf --mbtiles=guwahati.mbtiles --bounds=91.60,26.05,91.90,26.25 --max-zoom=14")
    print("4. Move guwahati.mbtiles to android/app/src/main/assets/map/guwahati.mbtiles")
    print("\nIn absence of MBTiles binary, S.E.N.T.I.N.E.L. runs with a built-in zero-network dark vector style with regional GeoJSON layers.")

if __name__ == "__main__":
    main()
