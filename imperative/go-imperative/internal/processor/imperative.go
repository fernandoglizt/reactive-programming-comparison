package processor

import (
	"context"
	"log/slog"
	"sync"
	"sync/atomic"
	"time"

	"github.com/fernandoglizt/imperative/go-imperative/internal/httpclient"
	"github.com/fernandoglizt/imperative/go-imperative/internal/model"
	"github.com/fernandoglizt/imperative/go-imperative/internal/util"
)

type Processor struct {
	httpClient   *httpclient.HttpClient
	concurrency  int
}

func NewProcessor(httpClient *httpclient.HttpClient, concurrency int) *Processor {
	return &Processor{
		httpClient:  httpClient,
		concurrency: concurrency,
	}
}

func (p *Processor) Process(ctx context.Context, req *model.ProcessRequest) model.ProcessResponse {
	startTime := time.Now()

	// Generate and transform items
	items := make([]int, 0, req.Count)
	for i := 1; i <= req.Count; i++ {
		val := i * 2
		if val%2 == 0 {
			items = append(items, val)
		}
	}

	processedEvents := len(items)
	batches := (processedEvents + req.Batch - 1) / req.Batch

	// Counters for results
	var externalCallsOk atomic.Int32
	var externalCallsFail atomic.Int32

	// Channel for work items
	workChan := make(chan int, p.concurrency)
	
	// WaitGroup for workers
	var wg sync.WaitGroup

	// Start worker goroutines
	for i := 0; i < p.concurrency; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for range workChan {
				url := util.AppendDelayParam(req.GetDownstreamUrl(), req.GetIoDelayMs())
				success := p.httpClient.CallWithRetry(ctx, url)
				
				if success {
					externalCallsOk.Add(1)
				} else {
					externalCallsFail.Add(1)
				}
			}
		}()
	}

	// Send work to workers
	for range items {
		workChan <- 1 // value doesn't matter, just need to trigger work
	}
	close(workChan)

	// Wait for all workers to finish
	wg.Wait()

	durationMs := time.Since(startTime).Milliseconds()

	response := model.NewSuccessResponse(
		req,
		processedEvents,
		batches,
		int(externalCallsOk.Load()),
		int(externalCallsFail.Load()),
		durationMs,
	)

	slog.Info("Processed events",
		"processed", processedEvents,
		"duration_ms", durationMs,
		"events_per_sec", response.EventsPerSec,
		"ok", externalCallsOk.Load(),
		"fail", externalCallsFail.Load(),
	)

	return response
}

