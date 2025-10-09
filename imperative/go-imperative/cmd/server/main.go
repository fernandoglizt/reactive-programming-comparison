package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/fernandoglizt/imperative/go-imperative/internal/handlers"
	"github.com/fernandoglizt/imperative/go-imperative/internal/httpclient"
	"github.com/fernandoglizt/imperative/go-imperative/internal/processor"
	"github.com/fernandoglizt/imperative/go-imperative/internal/util"
)

func main() {
	// Setup structured logging
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// Load configuration from environment
	port := util.GetEnvInt("PORT", 8093)
	maxCount := util.GetEnvInt("MAX_COUNT", 200000)
	concurrency := util.GetEnvInt("CONCURRENCY", 128)
	timeoutMs := util.GetEnvInt("DOWNSTREAM_TIMEOUT_MS", 2000)
	retryAttempts := util.GetEnvInt("RETRY_ATTEMPTS", 1)

	slog.Info("Starting Go Imperative server",
		"port", port,
		"maxCount", maxCount,
		"concurrency", concurrency,
		"timeoutMs", timeoutMs,
		"retryAttempts", retryAttempts,
	)

	// Create HTTP client
	httpClient := httpclient.NewHttpClient(timeoutMs, concurrency, retryAttempts)

	// Create processor
	proc := processor.NewProcessor(httpClient, concurrency)

	// Setup routes
	mux := http.NewServeMux()
	mux.Handle("/process", handlers.NewProcessHandler(proc, maxCount))
	mux.HandleFunc("/healthz", handlers.HealthHandler)
	mux.HandleFunc("/info", handlers.InfoHandler(maxCount))

	// Create server
	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: mux,
	}

	// Start server in goroutine
	go func() {
		slog.Info("Server listening", "port", port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("Server error", "error", err)
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	slog.Info("Shutting down server...")

	// Graceful shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		slog.Error("Server forced to shutdown", "error", err)
	}

	slog.Info("Server stopped")
}

