# S.E.N.T.I.N.E.L. API Specification

This document specifies the REST API endpoints and data schemas for the S.E.N.T.I.N.E.L. backend emergency service.

## Base URL
Default local development URL: `http://localhost:8080` or `http://10.0.2.2:8080` (from Android emulator).

---

## Data Models

### Medical Summary Object
```json
{
  "bloodGroup": "O+",
  "allergies": "Penicillin",
  "conditions": "Asthma",
  "contact": "+91-9876543210"
}
```
*Note: May be `null` if the user has not configured medical ID or opted out.*

### SOS Object
```json
{
  "id": "uuid",
  "ts": 1730900000000,
  "lat": 26.14,
  "lon": 91.73,
  "hops": 0,
  "name": "optional",
  "medical": {
    "bloodGroup": "O+",
    "allergies": "...",
    "conditions": "...",
    "contact": "..."
  }
}
```
*Fields:*
- `id` (string, required): Unique UUID identifier for deduplicating SOS signals across mesh relays.
- `ts` (int64, required): Unix timestamp in milliseconds when the emergency alert was triggered.
- `lat` (float64, required): Latitude coordinate (-90.0 to 90.0).
- `lon` (float64, required): Longitude coordinate (-180.0 to 180.0).
- `hops` (int, required): Number of mesh peer hops traversed before reaching an online node (0 if directly uploaded).
- `name` (string, optional): Display name of the user or device.
- `medical` (object or null, optional): Emergency medical profile summary (`bloodGroup`, `allergies`, `conditions`, `contact`).

---

## Endpoints

### 1. Health Check
Checks backend service availability.

- **Method:** `GET`
- **Path:** `/health`
- **Response Headers:** `Content-Type: application/json`
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "status": "ok"
}
```

---

### 2. Submit SOS Alert
Accepts an SOS emergency alert broadcast. Deduplicates entries in memory by `id`.

- **Method:** `POST`
- **Path:** `/sos`
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "id": "uuid",
  "ts": 1730900000000,
  "lat": 26.14,
  "lon": 91.73,
  "hops": 0,
  "name": "optional",
  "medical": {
    "bloodGroup": "O+",
    "allergies": "...",
    "conditions": "...",
    "contact": "..."
  }
}
```
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "ok": true
}
```

---

### 3. Retrieve SOS Alerts
Lists all stored SOS signals, sorted newest first (descending by `ts`).

- **Method:** `GET`
- **Path:** `/sos`
- **Response Headers:** `Content-Type: application/json`
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "id": "uuid",
    "ts": 1730900000000,
    "lat": 26.14,
    "lon": 91.73,
    "hops": 0,
    "name": "optional",
    "medical": {
      "bloodGroup": "O+",
      "allergies": "...",
      "conditions": "...",
      "contact": "..."
    }
  }
]
```

---

### 4. Emergency Dashboard
Web dashboard for rescue coordinators and incident commanders. Automatically polls `GET /sos` every 3 seconds and displays a live table of incoming distress signals.

- **Method:** `GET`
- **Path:** `/dashboard`
- **Response Headers:** `Content-Type: text/html; charset=utf-8`
- **Response Status:** `200 OK`
- **Displayed Table Columns:**
  - Time (Formatted UTC/local)
  - Lat / Lon
  - Name
  - Medical Summary
  - Hops

---

### 5. Emergency Triage Bot Chat
Online AI assistant providing urgent guidance for triage and first aid. Evaluates situation criticality and returns calm, numbered action steps.

- **Method:** `POST`
- **Path:** `/chat`
- **Request Headers:**
  - `Content-Type: application/json`
- **Constraints & Policies:**
  - Rate limit: 20 requests per minute per IP (returns `429 Too Many Requests` with `Retry-After` header).
  - Maximum message length: 2,000 characters per message.
  - History retention: Only the last 10 messages are submitted to the upstream model.
  - Upstream timeout: 20 seconds (returns `502 Bad Gateway` on upstream failure).

#### Request Body
```json
{
  "lang": "en",
  "messages": [
    {
      "role": "user",
      "content": "A person fell from a bike, has deep bleeding on their arm and says their chest hurts."
    }
  ]
}
```
*Parameters:*
- `lang` (string, optional): Language code (`"en"` or `"hi"`, defaults to `"en"`).
- `messages` (array, required): Array of message objects with `role` (`"user"` or `"assistant"`) and `content` (string). Cannot be empty or blank.

#### Response Status: `200 OK`
```json
{
  "reply": "1. Call 112 (or 108 for ambulance) immediately as chest pain and deep bleeding can be life-threatening.\n2. Apply firm, direct pressure to the arm wound using a clean cloth or sterile gauze.\n3. Keep the person sitting upright and still to ease breathing.\n4. Do not offer food or drinks while awaiting emergency responders.",
  "critical": true
}
```
*Parameters:*
- `reply` (string): Actionable, calm first-aid instructions stripped of internal triage control tags.
- `critical` (boolean): Flag indicating whether life-threatening conditions (e.g., severe bleeding, unconsciousness, chest pain, choking, snake bite) were identified by the model or keyword safety fallback.

#### Error Responses
- `400 Bad Request`: Empty input, blank message, or message exceeding 2,000 characters.
  ```json
  { "error": "Messages array cannot be empty." }
  ```
- `429 Too Many Requests`: Rate limit exceeded (> 20 requests / min).
  ```json
  { "error": "Rate limit exceeded. Please wait before submitting additional emergency queries." }
  ```
- `502 Bad Gateway`: Upstream LLM provider failure or timeout.
  ```json
  { "error": "The first-aid triage assistant is temporarily unavailable. If this is an emergency, please call 112 or 108 immediately." }
  ```

#### Example `curl` Requests

##### English Emergency Query
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "lang": "en",
    "messages": [
      {
        "role": "user",
        "content": "Elderly man is having intense chest pain and shortness of breath"
      }
    ]
  }'
```

##### Hindi Emergency Query (हिन्दी)
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "lang": "hi",
    "messages": [
      {
        "role": "user",
        "content": "खेत में एक व्यक्ति को सांप ने काट लिया है और पैर में सूजन आ रही है"
      }
    ]
  }'
```

