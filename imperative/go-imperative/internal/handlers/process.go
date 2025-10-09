package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/fernandoglizt/imperative/go-imperative/internal/model"
	"github.com/fernandoglizt/imperative/go-imperative/internal/processor"
)

type ProcessHandler struct {
	processor *processor.Processor
	maxCount  int
}

func NewProcessHandler(processor *processor.Processor, maxCount int) *ProcessHandler {
	return &ProcessHandler{
		processor: processor,
		maxCount:  maxCount,
	}
}

func (h *ProcessHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req model.ProcessRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	if err := req.Validate(h.maxCount); err != nil {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": err.Error()})
		return
	}

	response := h.processor.Process(r.Context(), &req)
	respondJSON(w, http.StatusOK, response)
}

func HealthHandler(w http.ResponseWriter, r *http.Request) {
	respondJSON(w, http.StatusOK, map[string]interface{}{
		"ok": true,
		"ts": time.Now().Format(time.RFC3339),
	})
}

func InfoHandler(maxCount int) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		respondJSON(w, http.StatusOK, map[string]interface{}{
			"name":       "go-imperative",
			"version":    "1.0.0",
			"goVersion":  "1.22",
			"maxCount":   maxCount,
			"notes":      "Imperative with goroutines and channels",
		})
	}
}

func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

