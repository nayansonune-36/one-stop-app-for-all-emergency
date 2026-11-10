package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"sort"
	"sync"
	"time"
)

// MedicalSummary represents the user's emergency medical profile.
type MedicalSummary struct {
	BloodGroup string `json:"bloodGroup,omitempty"`
	Allergies  string `json:"allergies,omitempty"`
	Conditions string `json:"conditions,omitempty"`
	Contact    string `json:"contact,omitempty"`
}

// SOSAlert represents an incoming emergency distress beacon.
type SOSAlert struct {
	ID      string          `json:"id"`
	TS      int64           `json:"ts"`
	Lat     float64         `json:"lat"`
	Lon     float64         `json:"lon"`
	Hops    int             `json:"hops"`
	Name    string          `json:"name,omitempty"`
	Medical *MedicalSummary `json:"medical"`
}

// ChatMessage represents a single message in the triage chat conversation.
type ChatMessage struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}

// ChatRequest represents the payload for POST /chat.
type ChatRequest struct {
	Lang     string        `json:"lang"`
	Messages []ChatMessage `json:"messages"`
}

// ChatResponse represents the response payload for POST /chat.
type ChatResponse struct {
	Reply    string `json:"reply"`
	Critical bool   `json:"critical"`
}

// InMemoryStore holds SOS alerts in memory with concurrency protection.
type InMemoryStore struct {
	mu     sync.RWMutex
	alerts map[string]SOSAlert
}

func NewInMemoryStore() *InMemoryStore {
	return &InMemoryStore{
		alerts: make(map[string]SOSAlert),
	}
}

func (s *InMemoryStore) Save(alert SOSAlert) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.alerts[alert.ID]; exists {
		// Deduplicated: already stored
		return false
	}
	s.alerts[alert.ID] = alert
	return true
}

func (s *InMemoryStore) GetAllNewestFirst() []SOSAlert {
	s.mu.RLock()
	defer s.mu.RUnlock()

	result := make([]SOSAlert, 0, len(s.alerts))
	for _, alert := range s.alerts {
		result = append(result, alert)
	}

	sort.Slice(result, func(i, j int) bool {
		return result[i].TS > result[j].TS
	})

	return result
}

// loggingResponseWriter captures the status code for logging middleware.
type loggingResponseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (lrw *loggingResponseWriter) WriteHeader(code int) {
	lrw.statusCode = code
	lrw.ResponseWriter.WriteHeader(code)
}

// withLogging wraps a handler with HTTP request logging.
func withLogging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		lrw := &loggingResponseWriter{ResponseWriter: w, statusCode: http.StatusOK}
		next.ServeHTTP(lrw, r)
		log.Printf("[%s] %s %s - %d (%s)", r.Method, r.RequestURI, r.RemoteAddr, lrw.statusCode, time.Since(start))
	})
}

// withCORS adds CORS headers to responses.
func withCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

const dashboardHTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>S.E.N.T.I.N.E.L. Emergency Dispatch</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #0B1220;
      color: #FFFFFF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans", sans-serif;
      padding: 24px;
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 2px solid #1E293B;
      margin-bottom: 24px;
    }
    h1 {
      font-size: 24px;
      color: #E53935;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .badge {
      font-size: 12px;
      padding: 4px 10px;
      border-radius: 9999px;
      background: #121A2B;
      border: 1px solid #334155;
      color: #94A3B8;
    }
    .status-live {
      color: #22C55E;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      background: #22C55E;
      border-radius: 50%;
      box-shadow: 0 0 8px #22C55E;
    }
    .card {
      background: #121A2B;
      border-radius: 12px;
      border: 1px solid #1E293B;
      overflow: hidden;
      box-shadow: 0 10px 25px rgba(0,0,0,0.4);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    th {
      background: #0D1628;
      color: #94A3B8;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      padding: 14px 16px;
      border-bottom: 1px solid #1E293B;
    }
    td {
      padding: 14px 16px;
      border-bottom: 1px solid #1E293B;
      font-size: 14px;
      vertical-align: top;
    }
    tr:hover td {
      background: rgba(229, 57, 53, 0.05);
    }
    .time-cell {
      font-variant-numeric: tabular-nums;
      color: #CBD5E1;
      white-space: nowrap;
    }
    .coords-cell {
      font-family: monospace;
      color: #38BDF8;
    }
    .hops-tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      background: #1E293B;
      color: #F8FAFC;
    }
    .medical-box {
      font-size: 13px;
      line-height: 1.4;
      color: #E2E8F0;
    }
    .medical-blood {
      color: #EF4444;
      font-weight: bold;
    }
    .empty-state {
      text-align: center;
      padding: 48px;
      color: #64748B;
      font-style: italic;
    }
  </style>
</head>
<body>
  <header>
    <div>
      <h1><span>&#9888;</span> S.E.N.T.I.N.E.L. Emergency Dispatch Console</h1>
      <span class="badge">Live Triage & Incident Monitoring</span>
    </div>
    <div class="status-live">
      <span class="pulse-dot"></span> Polling every 3s
    </div>
  </header>

  <div class="card">
    <table>
      <thead>
        <tr>
          <th>Time</th>
          <th>Lat / Lon</th>
          <th>Name</th>
          <th>Medical Summary</th>
          <th>Hops</th>
        </tr>
      </thead>
      <tbody id="sos-table-body">
        <tr>
          <td colspan="5" class="empty-state">Awaiting incoming emergency beacons...</td>
        </tr>
      </tbody>
    </table>
  </div>

  <script>
    async function fetchSOS() {
      try {
        const res = await fetch('/sos');
        if (!res.ok) throw new Error('Network error');
        const list = await res.json();
        const tbody = document.getElementById('sos-table-body');
        if (!list || list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No emergency beacons active at this moment.</td></tr>';
          return;
        }

        tbody.innerHTML = list.map(item => {
          const dateStr = item.ts ? new Date(item.ts).toLocaleString() : 'N/A';
          const coords = (item.lat !== undefined && item.lon !== undefined)
            ? item.lat.toFixed(4) + ', ' + item.lon.toFixed(4)
            : 'Unknown';
          const name = item.name || 'Anonymous Citizen';

          let medHtml = '<span style="color:#64748B;">None provided</span>';
          if (item.medical) {
            const parts = [];
            if (item.medical.bloodGroup) parts.push('<span class="medical-blood">Blood: ' + item.medical.bloodGroup + '</span>');
            if (item.medical.allergies) parts.push('Allergies: ' + item.medical.allergies);
            if (item.medical.conditions) parts.push('Conditions: ' + item.medical.conditions);
            if (item.medical.contact) parts.push('Contact: ' + item.medical.contact);
            medHtml = parts.join('<br>') || 'None';
          }

          return '<tr>' +
            '<td class="time-cell">' + dateStr + '</td>' +
            '<td class="coords-cell">' + coords + '</td>' +
            '<td><strong>' + name + '</strong></td>' +
            '<td class="medical-box">' + medHtml + '</td>' +
            '<td><span class="hops-tag">' + (item.hops ?? 0) + ' hops</span></td>' +
            '</tr>';
        }).join('');
      } catch (err) {
        console.error('Failed to poll SOS alerts:', err);
      }
    }

    fetchSOS();
    setInterval(fetchSOS, 3000);
  </script>
</body>
</html>`

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	store := NewInMemoryStore()
	mux := http.NewServeMux()

	// GET /health -> {"status":"ok"}
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	// Root redirect to /dashboard
	mux.HandleFunc("GET /{$}", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/dashboard", http.StatusTemporaryRedirect)
	})

	// POST /sos -> accepts SOS JSON in docs/API.md, dedupes by id, stores in memory, returns {"ok":true}
	mux.HandleFunc("POST /sos", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		var alert SOSAlert
		if err := json.NewDecoder(r.Body).Decode(&alert); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			_ = json.NewEncoder(w).Encode(map[string]string{"error": "invalid request json"})
			return
		}

		if alert.ID == "" {
			w.WriteHeader(http.StatusBadRequest)
			_ = json.NewEncoder(w).Encode(map[string]string{"error": "id is required"})
			return
		}

		store.Save(alert)
		_ = json.NewEncoder(w).Encode(map[string]bool{"ok": true})
	})

	// GET /sos -> list of stored SOS, newest first
	mux.HandleFunc("GET /sos", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		alerts := store.GetAllNewestFirst()
		_ = json.NewEncoder(w).Encode(alerts)
	})

	// GET /dashboard -> HTML page that polls GET /sos every 3 seconds
	mux.HandleFunc("GET /dashboard", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		_, _ = w.Write([]byte(dashboardHTML))
	})

	// Initialize LLM provider & IP rate limiter for POST /chat
	llmProvider, err := NewLLMProviderFromEnv()
	if err != nil {
		log.Fatalf("Fatal configuration error: %v. Please set LLM_API_KEY environment variable to start the backend.", err)
	}
	rateLimiter := NewIPRateLimiter(20, time.Minute)
	chatService := NewChatService(llmProvider, rateLimiter)

	// POST /chat -> first-aid triage assistant
	mux.HandleFunc("POST /chat", chatService.HandleChat)

	handler := withLogging(withCORS(mux))

	addr := ":" + port
	log.Printf("S.E.N.T.I.N.E.L. backend server listening on %s", addr)
	if err := http.ListenAndServe(addr, handler); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
