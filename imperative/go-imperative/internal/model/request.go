package model

import "fmt"

type ProcessRequest struct {
	Count          int    `json:"count"`
	Batch          int    `json:"batch"`
	IoDelayMs      int    `json:"io_delay_ms"`
	DownstreamUrl  string `json:"downstream_url"`
}

func (r *ProcessRequest) Validate(maxCount int) error {
	if r.Count <= 0 {
		return fmt.Errorf("count must be > 0")
	}
	if r.Batch <= 0 {
		return fmt.Errorf("batch must be > 0")
	}
	if r.Count > maxCount {
		return fmt.Errorf("count exceeds maximum allowed: %d", maxCount)
	}
	return nil
}

func (r *ProcessRequest) GetDownstreamUrl() string {
	if r.DownstreamUrl == "" {
		return "http://slow-io:8080/slow"
	}
	return r.DownstreamUrl
}

func (r *ProcessRequest) GetIoDelayMs() int {
	if r.IoDelayMs < 0 {
		return 0
	}
	return r.IoDelayMs
}

