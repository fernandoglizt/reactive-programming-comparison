package model

type ProcessRequest struct {
	Count         int    `json:"count"`
	Batch         int    `json:"batch"`
	IODelayMs     int    `json:"io_delay_ms"`
	DownstreamURL string `json:"downstream_url"`
}

func (r *ProcessRequest) Validate(maxCount int) []string {
	var errors []string

	if r.Count <= 0 {
		errors = append(errors, "count must be > 0")
	}

	if r.Batch <= 0 {
		errors = append(errors, "batch must be > 0")
	}

	if r.Count > maxCount {
		errors = append(errors, "count exceeds maximum allowed")
	}

	return errors
}

func (r *ProcessRequest) Normalize() {
	if r.IODelayMs < 0 {
		r.IODelayMs = 0
	}

	if r.DownstreamURL == "" {
		r.DownstreamURL = "http://slow-io:8080/slow"
	}
}
