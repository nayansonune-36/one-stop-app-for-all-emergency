# S.E.N.T.I.N.E.L. Backend Deployment Guide

This guide describes how to get the S.E.N.T.I.N.E.L. Go emergency backend running at a public HTTPS URL as fast as possible for submission, testing, and production.

---

## Fastest Route First: Comparison Overview

| Method | Setup Time | Public HTTPS? | Cost | Cold Starts? | Best For |
|---|---|---|---|---|---|
| **Route 1: Local + Cloudflare Tunnel / ngrok** | **< 2 minutes** | Yes (`https://*.trycloudflare.com`) | Free (no card / no account needed) | None (instant response) | **Immediate submission deadline & live demo** |
| **Route 2: Render Free Web Service (Docker)** | **5-10 minutes** | Yes (`https://*.onrender.com`) | Free | Yes (~50s delay after 15m idle) | **Permanent cloud hosting** |

---

## Route 1 (Fastest): Local Server + Public HTTPS Tunnel

This is the fastest and most reliable route for submission deadlines. The Go backend runs locally on your machine while a tunnel provides an immediate, secure public `https://` endpoint that can be queried by Android devices, judges, and web browsers anywhere in the world.

### Step 1: Start the Backend Locally

The server listens on `PORT` (default `8080`) and requires `LLM_API_KEY`. It reads configuration strictly from environment variables:
- `PORT` (optional, default `8080`)
- `LLM_PROVIDER=openai` (default `openai`)
- `LLM_MODEL=gpt-4o-mini` (default `gpt-4o-mini`)
- `LLM_API_KEY` (required; server will cleanly fail at startup if missing)

#### Option A: Run Native Go (Recommended)

**On Windows (PowerShell):**
```powershell
# Set your environment variables (paste your actual OpenAI key)
$env:PORT = "8080"
$env:LLM_PROVIDER = "openai"
$env:LLM_MODEL = "gpt-4o-mini"
$env:LLM_API_KEY = "sk-your-openai-key-here"

# Navigate to backend and run
cd backend
go run .
```

**On Linux / macOS (Bash):**
```bash
export PORT=8080
export LLM_PROVIDER=openai
export LLM_MODEL=gpt-4o-mini
export LLM_API_KEY="sk-your-openai-key-here"

cd backend
go run .
```

#### Option B: Run via Docker

```bash
# Build the compact Alpine image
docker build -t sentinel-backend .

# Run container (non-root nobody user, port 8080)
docker run -d \
  --name sentinel-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  -e PORT=8080 \
  -e LLM_PROVIDER=openai \
  -e LLM_MODEL=gpt-4o-mini \
  -e LLM_API_KEY="sk-your-openai-key-here" \
  sentinel-backend
```

---

### Step 2: Expose Locally Running Server via HTTPS Tunnel

Choose either **Cloudflare Tunnel** (zero account required) or **ngrok**.

#### Choice 2A: Cloudflare Tunnel (`cloudflared`) — Instant, No Account Needed

Cloudflare provides instant public HTTPS URLs without requiring an account or authentication token.

1. **Install `cloudflared`:**
   - **Windows:** `winget install Cloudflare.cloudflared` (or download from [github.com/cloudflare/cloudflared/releases](https://github.com/cloudflare/cloudflared/releases))
   - **macOS:** `brew install cloudflare/cloudflare/cloudflared`
   - **Linux:** `curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb && sudo dpkg -i cloudflared.deb`

2. **Start Tunnel:**
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

3. **Copy your Public HTTPS URL:**
   `cloudflared` will print a URL such as:
   ```text
   +--------------------------------------------------------------------------------------------+
   |  Your quick Tunnel has been created! Visit it at (it may take some time to be reachable):  |
   |  https://emergency-triage-demo.trycloudflare.com                                           |
   +--------------------------------------------------------------------------------------------+
   ```
   Your backend is now live at `https://emergency-triage-demo.trycloudflare.com`!

---

#### Choice 2B: ngrok

1. **Install `ngrok`:**
   - **Windows:** `winget install ngrok`
   - **macOS:** `brew install ngrok/ngrok/ngrok`
   - **Linux:** `sudo snap install ngrok`

2. **Expose Port 8080:**
   ```bash
   ngrok http 8080
   ```

3. **Copy your Public Forwarding URL:**
   Look for the `Forwarding` line in the terminal output:
   ```text
   Forwarding   https://xyz123.ngrok-free.app -> http://localhost:8080
   ```

---

## Route 2: Deploy Dockerfile to a Free Cloud Host (Render)

If you need a 24/7 cloud-hosted backend without keeping your computer on, Render provides free Docker container hosting.

### Step 1: Push Repository to GitHub
Ensure all your backend changes are committed and pushed to your GitHub repository.

### Step 2: Create Web Service on Render
1. Go to the [Render Dashboard](https://dashboard.render.com/) and click **New +** -> **Web Service**.
2. Connect your Git repository.
3. Configure the service settings:
   - **Name:** `sentinel-backend` (or your choice)
   - **Region:** Choose closest to your target audience (e.g., Singapore / Frankfurt / Oregon)
   - **Branch:** `main` (or your active branch)
   - **Root Directory:** Leave blank (Render detects root `Dockerfile`) or enter `backend`
   - **Runtime:** **Docker**
   - **Instance Type:** **Free**

### Step 3: Configure Environment Variables on Render
Under the **Environment Variables** section, add the following variables:

| Key | Value | Notes |
|---|---|---|
| `PORT` | `8080` | Server HTTP port (Render also dynamically maps internal port) |
| `LLM_PROVIDER` | `openai` | LLM triage engine |
| `LLM_MODEL` | `gpt-4o-mini` | Small, ultra-fast model for first-aid emergency advice |
| `LLM_API_KEY` | `sk-your-openai-api-key` | Paste your secret key (**Render stores this encrypted**) |

Click **Create Web Service**. Render will automatically build the multi-stage Docker container and deploy it to a public URL:
`https://sentinel-backend-xxxx.onrender.com`

---

> [!WARNING]
> ### Crucial Note on Render Free Tier Cold Starts
> Render's free tier spins down inactive web services after **15 minutes of zero inbound traffic**.
> - When spun down, the next inbound request will trigger a cold start taking **50 to 70 seconds** to spin up the container.
> - **Submission Tip:** Before submitting your project or presenting to judges, send a ping (`curl https://sentinel-backend-xxxx.onrender.com/health`) 1 minute beforehand to ensure the container is warm and active.
> - Alternatively, configure a free external monitor (e.g. [UptimeRobot](https://uptimerobot.com) or [cron-job.org](https://cron-job.org)) to ping `/health` every 10 minutes to prevent the container from sleeping.
> - For live demonstrations where guaranteed zero-latency is critical, **Route 1 (Local + Cloudflare Tunnel)** is strongly recommended.

---

## Testing & Verifying All Endpoints

Once your service is running (locally or on your public HTTPS URL, replace `https://<YOUR-URL>` below):

### 1. Health Check (`GET /health`)
```bash
curl -i https://<YOUR-URL>/health
```
**Expected Response (`200 OK`):**
```json
{"status":"ok"}
```

### 2. Live Incident Dashboard (`GET /dashboard` or `GET /`)
Open `https://<YOUR-URL>/dashboard` in your browser.
- Displays the S.E.N.T.I.N.E.L. Live Emergency Dispatch Console.
- Auto-polls `GET /sos` every 3 seconds to reflect newly submitted distress beacons.
- Root path `GET /` automatically redirects to `/dashboard`.

### 3. Emergency SOS Ingestion (`POST /sos`)
Submit a sample mesh distress beacon:
```bash
curl -i -X POST https://<YOUR-URL>/sos \
  -H "Content-Type: application/json" \
  -d '{
    "id": "sos-test-001",
    "ts": 1730900000000,
    "lat": 26.1408,
    "lon": 91.7378,
    "hops": 0,
    "name": "Priya Sharma",
    "medical": {
      "bloodGroup": "O+",
      "allergies": "Penicillin",
      "conditions": "Asthma",
      "contact": "+91-9876543210"
    }
  }'
```
**Expected Response (`200 OK`):**
```json
{"ok":true}
```

### 4. Retrieve All SOS Alerts (`GET /sos`)
```bash
curl -i https://<YOUR-URL>/sos
```
**Expected Response (`200 OK`):**
A JSON array sorted newest first (descending by `ts`). Notice how your newly posted beacon appears both in this JSON array and automatically appears on the `/dashboard` table.

### 5. Emergency AI Triage Chat (`POST /chat`)

#### English Emergency Query:
```bash
curl -i -X POST https://<YOUR-URL>/chat \
  -H "Content-Type: application/json" \
  -d '{
    "lang": "en",
    "messages": [
      {
        "role": "user",
        "content": "A person fell from a bicycle and is not breathing!"
      }
    ]
  }'
```
**Expected Response (`200 OK`):**
```json
{
  "reply": "1. Call 112 (or 108 for ambulance) immediately as the person is not breathing.\n2. Place the person flat on their back on a firm surface.\n3. Begin chest compressions immediately: place hands in the center of the chest and push hard and fast (100-120 beats per minute).\n4. Continue CPR until emergency medical responders arrive.",
  "critical": true
}
```
*Notice: `critical` is automatically set to `true` via OpenAI prompt instructions and our keyword safety fallback.*

#### Hindi Emergency Query (हिन्दी):
```bash
curl -i -X POST https://<YOUR-URL>/chat \
  -H "Content-Type: application/json; charset=utf-8" \
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
**Expected Response (`200 OK`):**
Returns clear, Devanagari Hindi instructions with `critical: true`.

---

## Security & Reliability Architecture

- **No Credential Leaks:** `LLM_API_KEY` is strictly server-side. It is never logged in standard access logs, never returned in API responses, and never bundled in client code.
- **Fail-Safe Startup:** The server validates that `LLM_API_KEY` is present at launch and exits immediately with an informative message if missing.
- **Graceful Error Handling (502 Bad Gateway):** If OpenAI's API experiences an outage, network drop, or rate limit, the server times out at 20 seconds and returns a clean, friendly JSON error advising the user to dial 112/108 immediately.
- **IP Rate Limiting:** Enforces a maximum of 20 requests per minute per IP address, protecting your OpenAI quota from abuse with `429 Too Many Requests` and a `Retry-After` header.
- **Hardened Container:** Docker container uses a multi-stage Alpine build (~15 MB), runs under an unprivileged user (`nobody:nobody`), and dynamically respects the `PORT` environment variable.
