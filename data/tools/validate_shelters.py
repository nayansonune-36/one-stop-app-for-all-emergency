#!/usr/bin/env python3
"""
S.E.N.T.I.N.E.L. Shelter Dataset Validator
Validates data/shelters.json against required schemas and business constraints:
1. Valid JSON formatting.
2. Unique identifier per entry (no duplicates).
3. Required fields present on every entry:
   id, name, name_hi, type, lat, lon, capacity (nullable), address, phone (nullable), source, verified, state, coord_precision.
4. Valid type: must be one of {'hospital', 'shelter', 'relief_camp'}.
5. Coordinates within bounding box of India: lat in [6.0, 37.0], lon in [68.0, 98.0].
6. Devanagari script present in name_hi (Unicode block U+0900 to U+097F).
7. coord_precision must be either 'exact' or 'approximate'.
"""

import sys
import os
import json
import re

REQUIRED_FIELDS = [
    "id",
    "name",
    "name_hi",
    "type",
    "lat",
    "lon",
    "capacity",
    "address",
    "phone",
    "source",
    "verified",
    "state",
    "coord_precision",
]

ALLOWED_TYPES = {"hospital", "shelter", "relief_camp"}
ALLOWED_PRECISION = {"exact", "approximate"}
DEVANAGARI_REGEX = re.compile(r"[\u0900-\u097F]")

def validate_shelters(file_path):
    print(f"Validating shelter dataset: {file_path}")
    if not os.path.exists(file_path):
        print(f"[FAIL] File not found: {file_path}")
        return False

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"[FAIL] Invalid JSON syntax: {e}")
        return False
    except Exception as e:
        print(f"[FAIL] Could not read file: {e}")
        return False

    if not isinstance(data, list):
        print("[FAIL] Root JSON structure must be an array of objects.")
        return False

    errors = []
    seen_ids = set()

    for idx, item in enumerate(data):
        entry_desc = f"Entry #{idx + 1}"
        if not isinstance(item, dict):
            errors.append(f"{entry_desc}: item is not a JSON object.")
            continue

        item_id = item.get("id")
        if item_id:
            entry_desc += f" (id: '{item_id}')"

        # Check required fields
        for field in REQUIRED_FIELDS:
            if field not in item:
                errors.append(f"{entry_desc}: missing required field '{field}'.")

        # Duplicate ID check
        if item_id:
            if item_id in seen_ids:
                errors.append(f"{entry_desc}: duplicate id '{item_id}'.")
            seen_ids.add(item_id)
        else:
            errors.append(f"{entry_desc}: 'id' is empty or missing.")

        # Type check
        stype = item.get("type")
        if stype not in ALLOWED_TYPES:
            errors.append(
                f"{entry_desc}: invalid type '{stype}'. Allowed types: {sorted(list(ALLOWED_TYPES))}."
            )

        # Coordinate bounds check
        lat = item.get("lat")
        lon = item.get("lon")
        if not isinstance(lat, (int, float)) or not isinstance(lon, (int, float)):
            errors.append(f"{entry_desc}: lat and lon must be numbers. Got lat={lat}, lon={lon}.")
        else:
            if not (6.0 <= lat <= 37.0):
                errors.append(f"{entry_desc}: lat {lat} outside India range [6.0, 37.0].")
            if not (68.0 <= lon <= 98.0):
                errors.append(f"{entry_desc}: lon {lon} outside India range [68.0, 98.0].")

        # Devanagari script check for name_hi
        name_hi = item.get("name_hi")
        if not name_hi or not isinstance(name_hi, str) or not DEVANAGARI_REGEX.search(name_hi):
            errors.append(
                f"{entry_desc}: 'name_hi' ('{name_hi}') must contain Devanagari script."
            )

        # coord_precision check
        precision = item.get("coord_precision")
        if precision not in ALLOWED_PRECISION:
            errors.append(
                f"{entry_desc}: invalid coord_precision '{precision}'. Must be one of {sorted(list(ALLOWED_PRECISION))}."
            )

    print(f"Total entries inspected: {len(data)}")
    if errors:
        print(f"\n[FAILURE] Found {len(errors)} validation issue(s):")
        for err in errors:
            print(f"  - {err}")
        return False
    else:
        print("\n[SUCCESS] All shelter entries passed validation with zero errors!")
        return True

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(__file__), "..", "shelters.json"
    )
    success = validate_shelters(target)
    sys.exit(0 if success else 1)
