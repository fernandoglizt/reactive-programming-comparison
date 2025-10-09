package main

import (
	"context"
	"go-rxgo/internal/handlers"
	"go-rxgo/internal/httpclient"
	"go-rxgo/internal/pipeline"
	"go-rxgo/internal/util"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	config := util.LoadConfig()

	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: parseLogLevel(config.LogLevel),
	}))

	logger.Info("Starting go-rxgo service",
		"port", config.Port,
		"maxCount", config.MaxCount,
		"batchConcurrency", config.BatchConcurrency,
		"itemConcurrency", config.ItemConcurrency,
		"downstreamTimeoutMs", config.DownstreamTimeoutMs,
		"retryAttempts", config.RetryAttempts)

	httpClient := httpclient.NewClient(
		config.ItemConcurrency,
		config.DownstreamTimeoutMs,
		config.RetryAttempts,
	)

	processor := pipeline.NewProcessor(
		httpClient,
		config.BatchConcurrency,
		config.ItemConcurrency,
	)

	processHandler := handlers.NewProcessHandler(processor, config.MaxCount)

	mux := http.NewServeMux()
	mux.HandleFunc("/process", processHandler.Process)
	mux.HandleFunc("/healthz", processHandler.Health)
	mux.HandleFunc("/info", processHandler.Info)

	server := &http.Server{
		Addr:    ":" + config.Port,
		Handler: mux,
	}

	go func() {
		logger.Info("Server starting", "addr", server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("Server failed to start", "error", err)
			os.Exit(1)
		}
	}()

	logger.Info("go-rxgo service started successfully")

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		logger.Error("Server forced to shutdown", "error", err)
		os.Exit(1)
	}

	logger.Info("Server exited")
}

func parseLogLevel(level string) slog.Level {
	switch level {
	case "DEBUG":
		return slog.LevelDebug
	case "INFO":
		return slog.LevelInfo
	case "WARN":
		return slog.LevelWarn
	case "ERROR":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}
