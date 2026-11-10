package main

import (
	"net"
	"net/http"
	"strings"
	"sync"
	"time"
)

// IPRateLimiter tracks request rates per client IP address.
type IPRateLimiter struct {
	mu       sync.Mutex
	limit    int
	window   time.Duration
	requests map[string][]time.Time
}

// NewIPRateLimiter creates a new rate limiter (e.g., 20 requests per minute).
func NewIPRateLimiter(limit int, window time.Duration) *IPRateLimiter {
	limiter := &IPRateLimiter{
		limit:    limit,
		window:   window,
		requests: make(map[string][]time.Time),
	}

	// Background routine to cleanup inactive IP entries
	go limiter.cleanupLoop()

	return limiter
}

// Allow returns true if the IP has not exceeded the rate limit.
func (l *IPRateLimiter) Allow(ip string) (bool, time.Duration) {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := time.Now()
	cutoff := now.Add(-l.window)

	// Filter out timestamps outside the active sliding window
	var active []time.Time
	for _, ts := range l.requests[ip] {
		if ts.After(cutoff) {
			active = append(active, ts)
		}
	}

	if len(active) >= l.limit {
		oldest := active[0]
		retryAfter := oldest.Add(l.window).Sub(now)
		if retryAfter <= 0 {
			retryAfter = time.Second
		}
		l.requests[ip] = active
		return false, retryAfter
	}

	active = append(active, now)
	l.requests[ip] = active
	return true, 0
}

func (l *IPRateLimiter) cleanupLoop() {
	ticker := time.NewTicker(2 * time.Minute)
	defer ticker.Stop()

	for range ticker.C {
		l.mu.Lock()
		now := time.Now()
		cutoff := now.Add(-l.window)
		for ip, timestamps := range l.requests {
			var active []time.Time
			for _, ts := range timestamps {
				if ts.After(cutoff) {
					active = append(active, ts)
				}
			}
			if len(active) == 0 {
				delete(l.requests, ip)
			} else {
				l.requests[ip] = active
			}
		}
		l.mu.Unlock()
	}
}

// ExtractClientIP retrieves the real client IP address from standard headers or RemoteAddr.
func ExtractClientIP(r *http.Request) string {
	// Check X-Forwarded-For
	xff := r.Header.Get("X-Forwarded-For")
	if xff != "" {
		parts := strings.Split(xff, ",")
		clientIP := strings.TrimSpace(parts[0])
		if clientIP != "" {
			return clientIP
		}
	}

	// Check X-Real-IP
	xri := strings.TrimSpace(r.Header.Get("X-Real-IP"))
	if xri != "" {
		return xri
	}

	// Fallback to RemoteAddr
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err == nil && host != "" {
		return host
	}

	return r.RemoteAddr
}
