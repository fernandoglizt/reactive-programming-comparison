package util

import (
	"os"
	"strconv"
)

// Config holds application configuration from environment variables
type Config struct {
	Port                string
	MaxCount            int
	BatchConcurrency    int
	ItemConcurrency     int
	DownstreamTimeoutMs int
	RetryAttempts       int
	LogLevel            string
}

// LoadConfig loads configuration from environment variables
func LoadConfig() *Config {
	return &Config{
		Port:                getEnv("PORT", "8083"),
		MaxCount:            getEnvInt("MAX_COUNT", 200000),
		BatchConcurrency:    getEnvInt("BATCH_CONCURRENCY", 4),
		ItemConcurrency:     getEnvInt("ITEM_CONCURRENCY", 64),
		DownstreamTimeoutMs: getEnvInt("DOWNSTREAM_TIMEOUT_MS", 2000),
		RetryAttempts:       getEnvInt("RETRY_ATTEMPTS", 1),
		LogLevel:            getEnv("LOG_LEVEL", "INFO"),
	}
}

// getEnv gets an environment variable with a default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

// getEnvInt gets an environment variable as integer with a default value
func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intValue, err := strconv.Atoi(value); err == nil {
			return intValue
		}
	}
	return defaultValue
}
