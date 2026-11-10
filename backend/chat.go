package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"
)

const (
	MaxMessageLength = 2000
	MaxHistoryCount  = 10
)

// ChatService coordinates rate-limiting, triage processing, and LLM queries.
type ChatService struct {
	provider LLMProvider
	limiter  *IPRateLimiter
}

// NewChatService creates a new ChatService.
func NewChatService(provider LLMProvider, limiter *IPRateLimiter) *ChatService {
	if limiter == nil {
		limiter = NewIPRateLimiter(20, time.Minute)
	}
	return &ChatService{
		provider: provider,
		limiter:  limiter,
	}
}

func (s *ChatService) HandleChat(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	// 1. IP Rate Limiting (20 req/min)
	clientIP := ExtractClientIP(r)
	if allowed, retryAfter := s.limiter.Allow(clientIP); !allowed {
		seconds := int(retryAfter.Seconds())
		if seconds <= 0 {
			seconds = 1
		}
		w.Header().Set("Retry-After", fmt.Sprintf("%d", seconds))
		w.WriteHeader(http.StatusTooManyRequests)
		_ = json.NewEncoder(w).Encode(map[string]string{
			"error": "Rate limit exceeded. Please wait before submitting additional emergency queries.",
		})
		return
	}

	// 2. Decode Request Body
	var req ChatRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(map[string]string{
			"error": "Invalid request JSON payload.",
		})
		return
	}

	// 3. Reject Empty Input
	if len(req.Messages) == 0 {
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(map[string]string{
			"error": "Messages array cannot be empty.",
		})
		return
	}

	hasValidMessage := false
	for _, msg := range req.Messages {
		trimmed := strings.TrimSpace(msg.Content)
		if trimmed != "" {
			hasValidMessage = true
		}
		// Validate individual message length
		if len([]rune(msg.Content)) > MaxMessageLength {
			w.WriteHeader(http.StatusBadRequest)
			_ = json.NewEncoder(w).Encode(map[string]string{
				"error": fmt.Sprintf("Message exceeds maximum length of %d characters.", MaxMessageLength),
			})
			return
		}
	}

	if !hasValidMessage {
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(map[string]string{
			"error": "Message content cannot be blank.",
		})
		return
	}

	// 4. Truncate History to Last 10 Messages
	messages := req.Messages
	if len(messages) > MaxHistoryCount {
		messages = messages[len(messages)-MaxHistoryCount:]
	}

	// 5. Build Localized System Prompt
	systemPrompt := GetSystemPrompt(req.Lang)

	// 6. Upstream LLM Call with 20s Context Timeout
	ctx, cancel := context.WithTimeout(r.Context(), 20*time.Second)
	defer cancel()

	rawReply, err := s.provider.GenerateReply(ctx, systemPrompt, messages)
	if err != nil {
		// Log internal error safely without leaking credentials
		log.Printf("[Chat ERROR] Upstream LLM call failed for client %s: %v", clientIP, err)
		w.WriteHeader(http.StatusBadGateway)
		_ = json.NewEncoder(w).Encode(map[string]string{
			"error": "The first-aid triage assistant is temporarily unavailable. If this is an emergency, please call 112 or 108 immediately.",
		})
		return
	}

	// 7. Parse CRITICAL Tag and Apply Keyword Fallback
	cleanReply, isCritical := ProcessTriageResponse(messages, rawReply)

	resp := ChatResponse{
		Reply:    cleanReply,
		Critical: isCritical,
	}

	_ = json.NewEncoder(w).Encode(resp)
}
