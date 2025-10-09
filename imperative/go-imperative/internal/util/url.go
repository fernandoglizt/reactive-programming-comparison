package util

import (
	"fmt"
	"strings"
)

func AppendDelayParam(url string, delayMs int) string {
	if strings.Contains(url, "?") {
		return fmt.Sprintf("%s&delay_ms=%d", url, delayMs)
	}
	return fmt.Sprintf("%s?delay_ms=%d", url, delayMs)
}

