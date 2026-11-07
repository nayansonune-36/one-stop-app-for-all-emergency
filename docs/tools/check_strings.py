#!/usr/bin/env python3
"""
S.E.N.T.I.N.E.L. Localization & String Verification Tool
Validates that:
1. Every string key in res/values/strings.xml exists in res/values-hi/strings.xml (and vice versa).
2. No string values are empty.
3. Every R.string.<id> referenced in Kotlin files exists in strings.xml.
"""

import sys
import os
import re
import xml.etree.ElementTree as ET

def get_keys_from_xml(filepath):
    if not os.path.exists(filepath):
        print(f"Error: File not found: {filepath}")
        sys.exit(1)
    tree = ET.parse(filepath)
    root = tree.getroot()
    keys = {}
    for elem in root.findall('string'):
        name = elem.get('name')
        text = elem.text or ""
        keys[name] = text
    return keys

def find_kotlin_string_refs(src_dir):
    pattern = re.compile(r'R\.string\.([a-zA-Z0-9_]+)')
    refs = set()
    for root, _, files in os.walk(src_dir):
        for f in files:
            if f.endswith('.kt'):
                with open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore') as kf:
                    content = kf.read()
                    matches = pattern.findall(content)
                    refs.update(matches)
    return refs

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    en_path = os.path.join(repo_root, "android", "app", "src", "main", "res", "values", "strings.xml")
    hi_path = os.path.join(repo_root, "android", "app", "src", "main", "res", "values-hi", "strings.xml")
    kt_src = os.path.join(repo_root, "android", "app", "src", "main", "java")

    print(f"Checking localization sync...")
    print(f"EN: {en_path}")
    print(f"HI: {hi_path}")

    en_keys = get_keys_from_xml(en_path)
    hi_keys = get_keys_from_xml(hi_path)

    print(f"\nTotal English strings: {len(en_keys)}")
    print(f"Total Hindi strings:   {len(hi_keys)}")

    missing_in_hi = set(en_keys.keys()) - set(hi_keys.keys())
    missing_in_en = set(hi_keys.keys()) - set(en_keys.keys())

    has_errors = False

    if missing_in_hi:
        has_errors = True
        print(f"\n[FAIL] {len(missing_in_hi)} keys present in EN but MISSING in HI:")
        for k in sorted(missing_in_hi):
            print(f"  - {k}")

    if missing_in_en:
        has_errors = True
        print(f"\n[FAIL] {len(missing_in_en)} keys present in HI but MISSING in EN:")
        for k in sorted(missing_in_en):
            print(f"  - {k}")

    # Check for empty values
    empty_en = [k for k, v in en_keys.items() if not v.strip()]
    empty_hi = [k for k, v in hi_keys.items() if not v.strip()]

    if empty_en:
        has_errors = True
        print(f"\n[FAIL] Empty English strings: {empty_en}")
    if empty_hi:
        has_errors = True
        print(f"\n[FAIL] Empty Hindi strings: {empty_hi}")

    # Check Kotlin code references
    print(f"\nScanning Kotlin source code in {kt_src} for R.string references...")
    code_refs = find_kotlin_string_refs(kt_src)
    print(f"Found {len(code_refs)} distinct R.string references.")

    undefined_in_code = code_refs - set(en_keys.keys())
    if undefined_in_code:
        has_errors = True
        print(f"\n[FAIL] {len(undefined_in_code)} R.string references in code NOT defined in strings.xml:")
        for k in sorted(undefined_in_code):
            print(f"  - R.string.{k}")
    else:
        print("[PASS] All R.string references in Kotlin resolve to defined string resources.")

    if not has_errors:
        print("\n[SUCCESS] 100% localization parity! All string resources match perfectly between English and Hindi.")
        sys.exit(0)
    else:
        print("\n[FAILURE] Localization mismatches detected. Please resolve above issues.")
        sys.exit(1)

if __name__ == '__main__':
    main()
