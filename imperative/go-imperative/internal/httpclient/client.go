package httpclient

import (
	"context"
	"io"
	"net/http"
	"time"
)

type HttpClient struct {
	client         *http.Client
	retryAttempts  int
}

func NewHttpClient(timeoutMs int, maxConns int, retryAttempts int) *HttpClient {
	transport := &http.Transport{
		MaxIdleConns:        1024,
		MaxIdleConnsPerHost: maxConns,
		MaxConnsPerHost:     maxConns,
		IdleConnTimeout:     90 * time.Second,
	}

	return &HttpClient{
		client: &http.Client{
			Transport: transport,
			Timeout:   time.Duration(timeoutMs) * time.Millisecond,
		},
		retryAttempts: retryAttempts,
	}
}

func (c *HttpClient) CallWithRetry(ctx context.Context, url string) bool {
	for attempt := 0; attempt <= c.retryAttempts; attempt++ {
		req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
		if err != nil {
			continue
		}

		resp, err := c.client.Do(req)
		if err != nil {
			if attempt < c.retryAttempts {
				backoff := 100 * time.Millisecond * time.Duration(1<<uint(attempt))
				if backoff > 300*time.Millisecond {
					backoff = 300 * time.Millisecond
				}
				time.Sleep(backoff)
				continue
			}
			return false
		}

		// Discard body and close
		io.Copy(io.Discard, resp.Body)
		resp.Body.Close()

		if resp.StatusCode >= 200 && resp.StatusCode < 300 {
			return true
		}

		if attempt < c.retryAttempts {
			backoff := 100 * time.Millisecond * time.Duration(1<<uint(attempt))
			if backoff > 300*time.Millisecond {
				backoff = 300 * time.Millisecond
			}
			time.Sleep(backoff)
		}
	}

	return false
}

