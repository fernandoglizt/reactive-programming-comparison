package handlers

import (
	"encoding/json"
	"go-rxgo/internal/model"
	"go-rxgo/internal/pipeline"
	"log/slog"
	"net/http"
	"os"
	"runtime"
	"time"
)

type ProcessHandler struct {
	processor *pipeline.Processor
	maxCount  int
	logger    *slog.Logger
}

func NewProcessHandler(processor *pipeline.Processor, maxCount int) *ProcessHandler {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))

	return &ProcessHandler{
		processor: processor,
		maxCount:  maxCount,
		logger:    logger,
	}
}

func (h *ProcessHandler) Process(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req model.ProcessRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.logger.Warn("Invalid request body", "error", err)
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	req.Normalize()

	if errors := req.Validate(h.maxCount); len(errors) > 0 {
		h.logger.Warn("Validation failed", "errors", errors)
		errorResponse := map[string]interface{}{
			"error": errors[0],
		}
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(errorResponse)
		return
	}

	h.logger.Info("Processing request",
		"count", req.Count,
		"batch", req.Batch,
		"ioDelayMs", req.IODelayMs,
		"downstreamURL", req.DownstreamURL)

	response, err := h.processor.ProcessEvents(r.Context(), &req)
	if err != nil {
		h.logger.Error("Processing failed", "error", err)
		errorResponse := map[string]interface{}{
			"error": "Internal server error",
		}
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(errorResponse)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

func (h *ProcessHandler) Health(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	response := model.NewHealthResponse()
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

func (h *ProcessHandler) Info(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	response := model.NewInfoResponse(
		"0.0.1-SNAPSHOT",
		runtime.Version(),
		time.Now().UTC().Format(time.RFC3339),
		h.maxCount,
	)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}
