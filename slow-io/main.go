package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand/v2"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
)

const (
	defaultPort      = "8080"
	defaultDelayMs   = 50
	maxDelayMs       = 10_000 // 10s de proteção
)

type response struct {
	OK        bool   `json:"ok"`
	DelayMs   int    `json:"delay_ms"`
	JitterMs  int    `json:"jitter_ms"`
	Code      int    `json:"code"`
	Timestamp string `json:"ts"`
	Instance  string `json:"instance"`
	RequestID string `json:"request_id"`
	Path      string `json:"path"`
}

type logEntry struct {
	Level     string `json:"level"`
	Msg       string `json:"msg"`
	DelayMs   int    `json:"delay_ms"`
	JitterMs  int    `json:"jitter_ms"`
	Code      int    `json:"code"`
	Path      string `json:"path"`
	Method    string `json:"method"`
	RemoteIP  string `json:"remote_ip"`
	LatencyMs int64  `json:"latency_ms"`
	RequestID string `json:"request_id"`
	Time      string `json:"time"`
}

func getenvInt(key string, def int) int {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	i, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return i
}

func main() {
	port := strings.TrimSpace(os.Getenv("PORT"))
	if port == "" {
		port = defaultPort
	}

	baseDelay := getenvInt("BASE_DELAY_MS", defaultDelayMs)
	if baseDelay < 0 {
		baseDelay = 0
	}
	if baseDelay > maxDelayMs {
		baseDelay = maxDelayMs
	}

	instance := strings.TrimSpace(os.Getenv("INSTANCE_ID"))
	if instance == "" {
		instance = fmt.Sprintf("slow-io-%s", uuid.NewString())
	}

	mux := http.NewServeMux()

	mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]any{"ok": true, "ts": time.Now().Format(time.RFC3339Nano)})
	})

	// /slow?delay_ms=50&jitter_ms=20&code=200
	mux.HandleFunc("/slow", func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		reqID := r.Header.Get("X-Request-Id")
		if reqID == "" {
			reqID = uuid.NewString()
		}

		// parâmetros
		delayMs := baseDelay
		if q := r.URL.Query().Get("delay_ms"); q != "" {
			if i, err := strconv.Atoi(q); err == nil {
				delayMs = i
			}
		}
		if delayMs < 0 {
			delayMs = 0
		}
		if delayMs > maxDelayMs {
			delayMs = maxDelayMs
		}

		jitterMs := 0
		if q := r.URL.Query().Get("jitter_ms"); q != "" {
			if i, err := strconv.Atoi(q); err == nil && i >= 0 {
				jitterMs = i
			}
		}
		if jitterMs > 0 {
			delayMs += rand.IntN(jitterMs + 1) // 0..jitterMs
		}

		code := 200
		if q := r.URL.Query().Get("code"); q != "" {
			if i, err := strconv.Atoi(q); err == nil {
				code = i
			}
		}
		if code < 100 || code > 599 {
			code = 200
		}

		// Simula I/O bloqueante
		time.Sleep(time.Duration(delayMs) * time.Millisecond)

		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("X-Instance", instance)
		w.Header().Set("X-Request-Id", reqID)
		w.WriteHeader(code)

		resp := response{
			OK:        code >= 200 && code < 300,
			DelayMs:   delayMs,
			JitterMs:  jitterMs,
			Code:      code,
			Timestamp: time.Now().Format(time.RFC3339Nano),
			Instance:  instance,
			RequestID: reqID,
			Path:      r.URL.Path,
		}
		_ = json.NewEncoder(w).Encode(resp)

		// Log estruturado
		entry := logEntry{
			Level:     "info",
			Msg:       "served /slow",
			DelayMs:   delayMs,
			JitterMs:  jitterMs,
			Code:      code,
			Path:      r.URL.Path,
			Method:    r.Method,
			RemoteIP:  r.RemoteAddr,
			LatencyMs: time.Since(start).Milliseconds(),
			RequestID: reqID,
			Time:      time.Now().Format(time.RFC3339Nano),
		}
		b, _ := json.Marshal(entry)
		log.Println(string(b))
	})

	srv := &http.Server{
		Addr:              ":" + port,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	log.Printf("slow-io listening on :%s (BASE_DELAY_MS=%d)\n", port, baseDelay)
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server error: %v", err)
	}
}
