package util

import (
	"net/url"
	"strconv"
	"strings"
)

// AppendDelayParam appends delay_ms parameter to URL, preserving existing query parameters
func AppendDelayParam(baseURL string, delayMs int) (string, error) {
	u, err := url.Parse(baseURL)
	if err != nil {
		return "", err
	}

	// Add delay_ms parameter
	q := u.Query()
	q.Set("delay_ms", strconv.Itoa(delayMs))
	u.RawQuery = q.Encode()

	return u.String(), nil
}

// AppendDelayParamString appends delay_ms parameter to URL string, preserving existing query parameters
func AppendDelayParamString(baseURL string, delayMs int) string {
	delayStr := strconv.Itoa(delayMs)
	if strings.Contains(baseURL, "?") {
		return baseURL + "&delay_ms=" + delayStr
	}
	return baseURL + "?delay_ms=" + delayStr
}
