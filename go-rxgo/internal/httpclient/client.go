package httpclient

import (
	"context"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"time"
)

type Client struct {
	httpClient    *http.Client
	timeout       time.Duration
	retryAttempts int
}

func NewClient(itemConcurrency int, timeoutMs, retryAttempts int) *Client {
	transport := &http.Transport{
		MaxConnsPerHost:       itemConcurrency,
		MaxIdleConns:          1024,
		MaxIdleConnsPerHost:   itemConcurrency,
		IdleConnTimeout:       90 * time.Second,
		TLSHandshakeTimeout:   5 * time.Second,
		ExpectContinueTimeout: 1 * time.Second,
		ResponseHeaderTimeout: time.Duration(timeoutMs) * time.Millisecond,
		DisableCompression:    false,
	}
	return &Client{
		httpClient:    &http.Client{Transport: transport},
		timeout:       time.Duration(timeoutMs) * time.Millisecond,
		retryAttempts: retryAttempts,
	}
}

func (c *Client) CallDownstream(parent context.Context, downstreamURL string, delayMs int) bool {
	for attempt := 0; attempt <= c.retryAttempts; attempt++ {
		ctx, cancel := context.WithTimeout(parent, c.timeout)

		u, err := url.Parse(downstreamURL)
		if err != nil {
			cancel()
			return false
		}
		q := u.Query()
		q.Set("delay_ms", strconv.Itoa(delayMs))
		u.RawQuery = q.Encode()

		req, err := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
		if err != nil {
			cancel()
			return false
		}
		resp, err := c.httpClient.Do(req)
		cancel()

		if err == nil {
			ok := resp.StatusCode >= 200 && resp.StatusCode < 300
			io.Copy(io.Discard, resp.Body)
			resp.Body.Close()
			if ok {
				return true
			}
		}
		if attempt == c.retryAttempts {
			return false
		}
		backoff := time.Duration(100*(1<<attempt)) * time.Millisecond
		if backoff > 300*time.Millisecond {
			backoff = 300 * time.Millisecond
		}
		time.Sleep(backoff)
	}
	return false
}
