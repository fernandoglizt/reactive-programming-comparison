package model

import (
	"os"
	"time"
)

type ProcessResponse struct {
	Ok               bool    `json:"ok"`
	Count            int     `json:"count"`
	Batch            int     `json:"batch"`
	IoDelayMs        int     `json:"io_delay_ms"`
	DownstreamUrl    string  `json:"downstream_url"`
	ProcessedEvents  int     `json:"processed_events"`
	Batches          int     `json:"batches"`
	ExternalCallsOk  int     `json:"external_calls_ok"`
	ExternalCallsFail int    `json:"external_calls_fail"`
	DurationMs       int64   `json:"duration_ms"`
	EventsPerSec     float64 `json:"events_per_sec"`
	Ts               string  `json:"ts"`
	Node             string  `json:"node"`
	Notes            string  `json:"notes"`
}

func NewSuccessResponse(
	req *ProcessRequest,
	processedEvents int,
	batches int,
	externalCallsOk int,
	externalCallsFail int,
	durationMs int64,
) ProcessResponse {
	eventsPerSec := 0.0
	if durationMs > 0 {
		eventsPerSec = float64(processedEvents) * 1000.0 / float64(durationMs)
	}

	hostname, _ := os.Hostname()

	return ProcessResponse{
		Ok:                true,
		Count:             req.Count,
		Batch:             req.Batch,
		IoDelayMs:         req.GetIoDelayMs(),
		DownstreamUrl:     req.GetDownstreamUrl(),
		ProcessedEvents:   processedEvents,
		Batches:           batches,
		ExternalCallsOk:   externalCallsOk,
		ExternalCallsFail: externalCallsFail,
		DurationMs:        durationMs,
		EventsPerSec:      eventsPerSec,
		Ts:                time.Now().Format(time.RFC3339),
		Node:              hostname,
		Notes:             "imperative with goroutines and channels",
	}
}

