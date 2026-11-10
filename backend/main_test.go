package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestHealthCheck(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	req := httptest.NewRequest("GET", "/health", nil)
	rr := httptest.NewRecorder()
	mux.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rr.Code)
	}

	var res map[string]string
	if err := json.Unmarshal(rr.Body.Bytes(), &res); err != nil {
		t.Fatalf("failed to parse response: %v", err)
	}
	if res["status"] != "ok" {
		t.Fatalf("expected status ok, got %v", res["status"])
	}
}

func TestSOSStoreAndDeduplication(t *testing.T) {
	store := NewInMemoryStore()

	s1 := SOSAlert{
		ID:   "alert-1",
		TS:   1000,
		Lat:  26.14,
		Lon:  91.73,
		Hops: 0,
		Name: "Test 1",
	}
	s2 := SOSAlert{
		ID:   "alert-2",
		TS:   2000,
		Lat:  26.15,
		Lon:  91.74,
		Hops: 1,
		Name: "Test 2",
	}

	if !store.Save(s1) {
		t.Errorf("expected first save of alert-1 to succeed")
	}
	if store.Save(s1) {
		t.Errorf("expected duplicate save of alert-1 to be deduplicated (return false)")
	}
	if !store.Save(s2) {
		t.Errorf("expected save of alert-2 to succeed")
	}

	alerts := store.GetAllNewestFirst()
	if len(alerts) != 2 {
		t.Fatalf("expected 2 alerts, got %d", len(alerts))
	}
	if alerts[0].ID != "alert-2" || alerts[1].ID != "alert-1" {
		t.Errorf("expected newest first ordering: alert-2 then alert-1")
	}
}

func TestCriticalParsing(t *testing.T) {
	tests := []struct {
		name         string
		rawReply     string
		expectClean  string
		expectCrit   bool
	}{
		{
			name: "Critical true standard",
			rawReply: "1. Call 112 immediately.\n2. Keep airway clear.\nCRITICAL: true",
			expectClean: "1. Call 112 immediately.\n2. Keep airway clear.",
			expectCrit:  true,
		},
		{
			name: "Critical false standard with spacing",
			rawReply: "1. Call 112 if condition worsens.\n2. Apply bandage.\n\nCRITICAL: false\n",
			expectClean: "1. Call 112 if condition worsens.\n2. Apply bandage.",
			expectCrit:  false,
		},
		{
			name: "Critical with bold markdown",
			rawReply: "1. Keep patient calm.\n**CRITICAL: true**",
			expectClean: "1. Keep patient calm.",
			expectCrit:  true,
		},
		{
			name: "No critical tag defaults to false",
			rawReply: "1. Drink fluids.\n2. Rest in shade.",
			expectClean: "1. Drink fluids.\n2. Rest in shade.",
			expectCrit:  false,
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			clean, crit := ParseCritical(tc.rawReply)
			if clean != tc.expectClean {
				t.Errorf("expected clean reply %q, got %q", tc.expectClean, clean)
			}
			if crit != tc.expectCrit {
				t.Errorf("expected isCritical %v, got %v", tc.expectCrit, crit)
			}
		})
	}
}

func TestKeywordFallback(t *testing.T) {
	tests := []struct {
		name       string
		userText   string
		rawReply   string
		expectCrit bool
	}{
		{
			name:       "English: not breathing",
			userText:   "My friend is not breathing after falling!",
			rawReply:   "1. Call 112.\n2. Begin CPR.\nCRITICAL: false", // Model mistakenly said false
			expectCrit: true,                                            // Keyword overrides to true
		},
		{
			name:       "English: snake bite",
			userText:   "Someone got a snake bite on their leg",
			rawReply:   "1. Call 112.\n2. Immobilize leg.\nCRITICAL: false",
			expectCrit: true,
		},
		{
			name:       "English: chest pain",
			userText:   "Elderly man having severe chest pain",
			rawReply:   "1. Call 112.\n2. Sit upright.\nCRITICAL: false",
			expectCrit: true,
		},
		{
			name:       "Hindi: सांस नहीं आ रही (not breathing)",
			userText:   "रोगी को सांस नहीं आ रही है, क्या करूं?",
			rawReply:   "1. 112 पर कॉल करें।\n2. सीपीआर शुरू करें।\nCRITICAL: false",
			expectCrit: true,
		},
		{
			name:       "Hindi: बेहोश (unconscious)",
			userText:   "व्यक्ति अचानक बेहोश हो गया है",
			rawReply:   "1. 112 पर फोन करें।\nCRITICAL: false",
			expectCrit: true,
		},
		{
			name:       "Hindi: सांप ने काट लिया (snake bite)",
			userText:   "खेत में सांप ने काट लिया है",
			rawReply:   "1. 112 पर तुरंत संपर्क करें।\nCRITICAL: false",
			expectCrit: true,
		},
		{
			name:       "Non-critical query",
			userText:   "I have a minor paper cut on my index finger",
			rawReply:   "1. Wash with clean water.\n2. Apply small adhesive bandage.\nCRITICAL: false",
			expectCrit: false,
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			msgs := []ChatMessage{{Role: "user", Content: tc.userText}}
			_, crit := ProcessTriageResponse(msgs, tc.rawReply)
			if crit != tc.expectCrit {
				t.Errorf("expected isCritical=%v, got %v for text %q", tc.expectCrit, crit, tc.userText)
			}
		})
	}
}

func TestChatHandlerValidRequest(t *testing.T) {
	mock := &MockProvider{
		ReplyFunc: func(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
			return "1. Call 112 if life-threatening.\n2. Apply direct pressure with clean cloth.\nCRITICAL: true", nil
		},
	}

	limiter := NewIPRateLimiter(20, time.Minute)
	service := NewChatService(mock, limiter)

	payload := `{"lang":"en","messages":[{"role":"user","content":"Deep cut on arm"}]}`
	req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
	req.RemoteAddr = "192.168.1.1:12345"
	rr := httptest.NewRecorder()

	service.HandleChat(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d: %s", rr.Code, rr.Body.String())
	}

	var resp ChatResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &resp); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}

	if !resp.Critical {
		t.Errorf("expected critical=true")
	}

	if strings.Contains(resp.Reply, "CRITICAL:") {
		t.Errorf("expected 'CRITICAL:' tag to be stripped from reply, got: %s", resp.Reply)
	}
}

func TestChatHandlerEmptyInputValidation(t *testing.T) {
	mock := &MockProvider{}
	service := NewChatService(mock, NewIPRateLimiter(20, time.Minute))

	// Empty messages array
	{
		payload := `{"lang":"en","messages":[]}`
		req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
		rr := httptest.NewRecorder()
		service.HandleChat(rr, req)
		if rr.Code != http.StatusBadRequest {
			t.Errorf("expected 400 for empty messages, got %d", rr.Code)
		}
	}

	// Whitespace only message
	{
		payload := `{"lang":"en","messages":[{"role":"user","content":"   \n  "}]}`
		req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
		rr := httptest.NewRecorder()
		service.HandleChat(rr, req)
		if rr.Code != http.StatusBadRequest {
			t.Errorf("expected 400 for blank message content, got %d", rr.Code)
		}
	}
}

func TestChatHandlerMessageLengthLimit(t *testing.T) {
	mock := &MockProvider{}
	service := NewChatService(mock, NewIPRateLimiter(20, time.Minute))

	longText := strings.Repeat("a", 2001)
	reqObj := ChatRequest{
		Lang: "en",
		Messages: []ChatMessage{
			{Role: "user", Content: longText},
		},
	}
	body, _ := json.Marshal(reqObj)
	req := httptest.NewRequest("POST", "/chat", bytes.NewReader(body))
	rr := httptest.NewRecorder()

	service.HandleChat(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Errorf("expected 400 for message exceeding 2000 chars, got %d", rr.Code)
	}
}

func TestChatHandlerHistoryLimit(t *testing.T) {
	var capturedMessages []ChatMessage
	mock := &MockProvider{
		ReplyFunc: func(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
			capturedMessages = messages
			return "1. Call 112.\n2. Rest.\nCRITICAL: false", nil
		},
	}
	service := NewChatService(mock, NewIPRateLimiter(20, time.Minute))

	// Send 15 messages
	var msgs []ChatMessage
	for i := 1; i <= 15; i++ {
		msgs = append(msgs, ChatMessage{Role: "user", Content: "Msg"})
	}

	reqObj := ChatRequest{Lang: "en", Messages: msgs}
	body, _ := json.Marshal(reqObj)
	req := httptest.NewRequest("POST", "/chat", bytes.NewReader(body))
	rr := httptest.NewRecorder()

	service.HandleChat(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rr.Code)
	}

	if len(capturedMessages) != 10 {
		t.Errorf("expected history truncated to 10 messages, got %d", len(capturedMessages))
	}
}

func TestChatHandlerRateLimiting(t *testing.T) {
	mock := &MockProvider{}
	limiter := NewIPRateLimiter(3, time.Minute) // set limit to 3 for fast test
	service := NewChatService(mock, limiter)

	clientIP := "10.10.10.1:4000"

	for i := 0; i < 3; i++ {
		payload := `{"lang":"en","messages":[{"role":"user","content":"Help"}]}`
		req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
		req.RemoteAddr = clientIP
		rr := httptest.NewRecorder()
		service.HandleChat(rr, req)

		if rr.Code != http.StatusOK {
			t.Fatalf("expected request %d to succeed, got %d", i+1, rr.Code)
		}
	}

	// 4th request must be rate limited (429)
	payload := `{"lang":"en","messages":[{"role":"user","content":"Help again"}]}`
	req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
	req.RemoteAddr = clientIP
	rr := httptest.NewRecorder()
	service.HandleChat(rr, req)

	if rr.Code != http.StatusTooManyRequests {
		t.Errorf("expected 429 Too Many Requests, got %d", rr.Code)
	}

	var errResp map[string]string
	_ = json.Unmarshal(rr.Body.Bytes(), &errResp)
	if !strings.Contains(errResp["error"], "Rate limit exceeded") {
		t.Errorf("expected rate limit error message, got %v", errResp)
	}
}

func TestChatHandlerUpstreamFailure(t *testing.T) {
	mock := &MockProvider{
		ReplyFunc: func(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
			return "", errors.New("upstream timeout connection error")
		},
	}
	service := NewChatService(mock, NewIPRateLimiter(20, time.Minute))

	payload := `{"lang":"en","messages":[{"role":"user","content":"Help"}]}`
	req := httptest.NewRequest("POST", "/chat", strings.NewReader(payload))
	rr := httptest.NewRecorder()
	service.HandleChat(rr, req)

	if rr.Code != http.StatusBadGateway {
		t.Errorf("expected 502 Bad Gateway on upstream failure, got %d", rr.Code)
	}

	var errResp map[string]string
	_ = json.Unmarshal(rr.Body.Bytes(), &errResp)
	if !strings.Contains(errResp["error"], "112") {
		t.Errorf("expected friendly emergency advice in error response, got %v", errResp)
	}
}

func TestNewLLMProviderFromEnv(t *testing.T) {
	// Test missing key with default/openai provider
	t.Setenv("LLM_PROVIDER", "openai")
	t.Setenv("LLM_API_KEY", "")
	t.Setenv("OPENAI_API_KEY", "")
	_, err := NewLLMProviderFromEnv()
	if err == nil {
		t.Errorf("expected error when LLM_API_KEY is missing, got nil")
	}

	// Test mock provider succeeds without key
	t.Setenv("LLM_PROVIDER", "mock")
	prov, err := NewLLMProviderFromEnv()
	if err != nil {
		t.Fatalf("unexpected error for mock provider: %v", err)
	}
	if _, ok := prov.(*MockProvider); !ok {
		t.Errorf("expected *MockProvider, got %T", prov)
	}

	// Test openai provider with key configured
	t.Setenv("LLM_PROVIDER", "openai")
	t.Setenv("LLM_API_KEY", "test-key-do-not-log")
	t.Setenv("LLM_MODEL", "")
	prov, err = NewLLMProviderFromEnv()
	if err != nil {
		t.Fatalf("unexpected error with key set: %v", err)
	}
	openAIProv, ok := prov.(*OpenAICompatibleProvider)
	if !ok {
		t.Fatalf("expected *OpenAICompatibleProvider, got %T", prov)
	}
	if openAIProv.model != "gpt-4o-mini" {
		t.Errorf("expected default model 'gpt-4o-mini', got %q", openAIProv.model)
	}
	if openAIProv.baseURL != "https://api.openai.com/v1/chat/completions" {
		t.Errorf("expected default baseURL 'https://api.openai.com/v1/chat/completions', got %q", openAIProv.baseURL)
	}
}

func TestSOSHttpEndpoints(t *testing.T) {
	store := NewInMemoryStore()
	mux := http.NewServeMux()

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

	mux.HandleFunc("GET /sos", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		alerts := store.GetAllNewestFirst()
		_ = json.NewEncoder(w).Encode(alerts)
	})

	// 1. Initial GET /sos returns empty array []
	req := httptest.NewRequest("GET", "/sos", nil)
	rr := httptest.NewRecorder()
	mux.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rr.Code)
	}
	if strings.TrimSpace(rr.Body.String()) != "[]" {
		t.Errorf("expected empty array '[]', got %q", rr.Body.String())
	}

	// 2. POST /sos with valid payload
	body := `{"id":"sos-123","ts":1730900000000,"lat":26.14,"lon":91.73,"hops":0,"name":"Test User","medical":{"bloodGroup":"O+","allergies":"Penicillin","conditions":"Asthma","contact":"+91-9876543210"}}`
	req = httptest.NewRequest("POST", "/sos", strings.NewReader(body))
	rr = httptest.NewRecorder()
	mux.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", rr.Code, rr.Body.String())
	}
	var postRes map[string]bool
	_ = json.Unmarshal(rr.Body.Bytes(), &postRes)
	if !postRes["ok"] {
		t.Errorf("expected ok: true, got %v", postRes)
	}

	// 3. GET /sos returns the saved alert
	req = httptest.NewRequest("GET", "/sos", nil)
	rr = httptest.NewRecorder()
	mux.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rr.Code)
	}
	var alerts []SOSAlert
	if err := json.Unmarshal(rr.Body.Bytes(), &alerts); err != nil {
		t.Fatalf("failed to decode alerts: %v", err)
	}
	if len(alerts) != 1 || alerts[0].ID != "sos-123" {
		t.Errorf("expected 1 alert with ID 'sos-123', got %+v", alerts)
	}
}
