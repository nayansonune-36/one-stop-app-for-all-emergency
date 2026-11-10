# S.E.N.T.I.N.E.L. Data Schemas

This document defines the offline data schemas used by S.E.N.T.I.N.E.L., including shelters, hospitals, relief camps, and offline datasets.

## 1. Shelter Dataset (`/data/shelters.json`)

The `/data/shelters.json` file contains a curated offline registry of emergency points-of-interest (shelters, hospitals, relief camps) packaged with the application for offline disaster resilience across multiple states (Assam, Odisha, Kerala, Bihar, Tamil Nadu, and Maharashtra).

> [!IMPORTANT]
> **Sample Data & UI Representation Rule:**
> All shelter data bundled in `/data/shelters.json` is **sample data for demo and simulation purposes**.
> The data must **NEVER** be presented, labeled, or described anywhere in the UI as "live", "complete", or "verified". Remove any UI badges, wording, or timestamps that imply live or verified status.

---

### Schema Definition

The file must be a JSON array containing objects conforming to the following structure:

```json
[
  {
    "id": "string-unique",
    "name": "English name",
    "name_hi": "Hindi name",
    "type": "shelter",
    "lat": 26.1445,
    "lon": 91.7362,
    "capacity": 200,
    "address": "Street address or landmark",
    "phone": "string or null",
    "source": "Provenance URL or official document",
    "verified": "YYYY-MM-DD",
    "state": "Assam",
    "coord_precision": "exact",
    "data_notes": "Optional metadata note"
  }
]
```

---

### Field Specifications

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | Yes | Unique identifier slug (e.g., `"gmch-bhangagarh"`). |
| `name` | `string` | Yes | Name of the facility in English. |
| `name_hi` | `string` | Yes | Name of the facility in Hindi (Devanagari script). |
| `type` | `string` | Yes | Facility category. Must be one of: `"shelter"`, `"hospital"`, or `"relief_camp"`. |
| `lat` | `number (float)` | Yes | Latitude in decimal degrees (India range: `6.0` to `37.0`). |
| `lon` | `number (float)` | Yes | Longitude in decimal degrees (India range: `68.0` to `98.0`). |
| `capacity` | `integer` or `null` | Yes | Maximum occupancy capacity, or `null` if unknown. |
| `address` | `string` | Yes | Physical street address, landmark, or district location. |
| `phone` | `string` or `null` | Yes | Contact phone number, emergency hotline, or `null`. |
| `source` | `string` | Yes | Data provenance URL, government bulletin, or official registry link. |
| `verified` | `string (ISO-8601 Date)` | Yes | Archival ingestion date in `YYYY-MM-DD` format (internal audit metadata only; never shown as a "verified" badge in the UI). |
| `state` | `string` | Yes | State/UT name (e.g. `"Assam"`, `"Odisha"`, `"Kerala"`, `"Bihar"`, `"Tamil Nadu"`, `"Maharashtra"`). |
| `coord_precision` | `string` | Yes | Precision indicator: `"exact"` or `"approximate"`. See details below. |
| `data_notes` | `string` or `null` | No | Optional contextual notes regarding phone lines, control rooms, or data provenance. |

---

### Meaning of `coord_precision`

- **`"exact"`**: The latitude/longitude coordinate points directly to the physical facility building or entry gate. Distance calculations and nearest-shelter sorting are permitted only for `"exact"` entries.
- **`"approximate"`**: The coordinate represents a town, village, or neighborhood centroid, rather than the exact building structure.
  - Used when physical building geolocation is unavailable.
  - **Rules for `"approximate"` entries in the UI:**
    - Never display numeric distance (e.g. `"2.4 km away"`) for approximate entries.
    - Approximate entries must display an `"Approximate location"` badge.
    - Approximate entries are excluded from nearest-shelter distance calculations.
    - Overlapping coordinates (where multiple shelters share the same town centroid) must be clustered on the map.

---

### Phone & Control Room Classification

Some entries list administrative control room hotlines (e.g. `1077`, `1913`, DDMA lines) rather than the building's direct phone number. In the UI detail sheet:
- When a phone number is `1077`, `1913`, or `data_notes` indicates a control room, it must be clearly labeled as **"District control room"**.

---

### Example Entry

```json
[
  {
    "id": "gmch-bhangagarh",
    "name": "Gauhati Medical College and Hospital (GMCH)",
    "name_hi": "गौहाटी मेडिकल कॉलेज एवं अस्पताल (जीएमसीएच)",
    "type": "hospital",
    "lat": 26.159632,
    "lon": 91.766825,
    "capacity": 3169,
    "address": "Narakasur Hilltop, GMC Hospital Road, Bhangagarh, Guwahati, Assam 781032",
    "phone": "+91-361-2452244",
    "source": "https://gmch.assam.gov.in/about-us",
    "verified": "2026-11-08",
    "state": "Assam",
    "coord_precision": "exact",
    "data_notes": "Main tertiary care facility for Kamrup Metropolitan"
  }
]
```
