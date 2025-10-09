package model

import "time"

// ProcessResponse represents the response payload for the /process endpoint
type ProcessResponse struct {
	OK                bool    `json:"ok"`
	Count             int     `json:"count"`
	Batch             int     `json:"batch"`
	IODelayMs         int     `json:"io_delay_ms"`
	DownstreamURL     string  `json:"downstream_url"`
	ProcessedEvents   int     `json:"processed_events"`
	Batches           int     `json:"batches"`
	ExternalCallsOk   int     `json:"external_calls_ok"`
	ExternalCallsFail int     `json:"external_calls_fail"`
	DurationMs        int64   `json:"duration_ms"`
	EventsPerSec      float64 `json:"events_per_sec"`
	Timestamp         string  `json:"ts"`
	Node              string  `json:"node"`
	Notes             string  `json:"notes"`
}

// HealthResponse represents the health check response
type HealthResponse struct {
	OK bool   `json:"ok"`
	TS string `json:"ts"`
}

// InfoResponse represents the info endpoint response
type InfoResponse struct {
	Name      string `json:"name"`
	Version   string `json:"version"`
	GoVersion string `json:"goVersion"`
	BuildTime string `json:"buildTime"`
	MaxCount  int    `json:"maxCount"`
}

// NewHealthResponse creates a new health response
func NewHealthResponse() HealthResponse {
	return HealthResponse{
		OK: true,
		TS: time.Now().UTC().Format(time.RFC3339),
	}
}

// NewInfoResponse creates a new info response
func NewInfoResponse(version, goVersion, buildTime string, maxCount int) InfoResponse {
	return InfoResponse{
		Name:      "go-rxgo",
		Version:   version,
		GoVersion: goVersion,
		BuildTime: buildTime,
		MaxCount:  maxCount,
	}
}
