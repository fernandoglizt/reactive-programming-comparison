package pipeline

import (
	"context"
	"go-rxgo/internal/httpclient"
	"go-rxgo/internal/model"
	"log/slog"
	"os"
	"time"

	"github.com/reactivex/rxgo/v2"
)

type Metrics struct {
	ProcessedEvents   int
	ExternalCallsOk   int
	ExternalCallsFail int
}

type Processor struct {
	httpClient       *httpclient.Client
	batchConcurrency int
	itemConcurrency  int
	logger           *slog.Logger
}

func NewProcessor(httpClient *httpclient.Client, batchConcurrency, itemConcurrency int) *Processor {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))
	return &Processor{
		httpClient:       httpClient,
		batchConcurrency: batchConcurrency,
		itemConcurrency:  itemConcurrency,
		logger:           logger,
	}
}

func (p *Processor) ProcessEvents(ctx context.Context, req *model.ProcessRequest) (*model.ProcessResponse, error) {
	start := time.Now()
	host, _ := os.Hostname()

	p.logger.Info("Starting RxGo processing",
		"count", req.Count,
		"batch", req.Batch,
		"ioDelayMs", req.IODelayMs,
		"downstreamURL", req.DownstreamURL)

	items := make([]interface{}, req.Count)
	for i := 0; i < req.Count; i++ {
		items[i] = i + 1
	}

	base := rxgo.Just(items...)().
		Map(func(_ context.Context, v interface{}) (interface{}, error) {
			return v.(int) * 2, nil
		}).
		Filter(func(v interface{}) bool {
			return v.(int)%2 == 0
		})

	batched := base.BufferWithCount(req.Batch)

	perBatch := func(it rxgo.Item) rxgo.Observable {
		items := it.V.([]interface{})

		itemObservables := make([]rxgo.Observable, len(items))
		for i := range items {
			itemObservables[i] = rxgo.Create([]rxgo.Producer{
				func(_ context.Context, ch chan<- rxgo.Item) {
					ok := p.httpClient.CallDownstream(ctx, req.DownstreamURL, req.IODelayMs)
					ch <- rxgo.Of(ok)
				},
			})
		}

		return rxgo.Merge(itemObservables, rxgo.WithPool(p.itemConcurrency))
	}

	processed := batched.FlatMap(perBatch, rxgo.WithPool(p.batchConcurrency))

	metrics := &Metrics{}
	for out := range processed.Observe() {
		if out.E != nil {
			return nil, out.E
		}
		metrics.ProcessedEvents++
		if ok, _ := out.V.(bool); ok {
			metrics.ExternalCallsOk++
		} else {
			metrics.ExternalCallsFail++
		}
	}

	durMs := time.Since(start).Milliseconds()
	eventsPerSec := 0.0
	if durMs > 0 {
		eventsPerSec = float64(metrics.ProcessedEvents) * 1000.0 / float64(durMs)
	}
	batches := (metrics.ProcessedEvents + req.Batch - 1) / req.Batch

	p.logger.Info("RxGo processing completed",
		"processedEvents", metrics.ProcessedEvents,
		"externalCallsOk", metrics.ExternalCallsOk,
		"externalCallsFail", metrics.ExternalCallsFail,
		"durationMs", durMs,
		"eventsPerSec", eventsPerSec)

	return &model.ProcessResponse{
		OK:                metrics.ExternalCallsFail == 0,
		Count:             req.Count,
		Batch:             req.Batch,
		IODelayMs:         req.IODelayMs,
		DownstreamURL:     req.DownstreamURL,
		ProcessedEvents:   metrics.ProcessedEvents,
		Batches:           batches,
		ExternalCallsOk:   metrics.ExternalCallsOk,
		ExternalCallsFail: metrics.ExternalCallsFail,
		DurationMs:        durMs,
		EventsPerSec:      eventsPerSec,
		Timestamp:         start.UTC().Format(time.RFC3339),
		Node:              host,
		Notes:             "rxgo reactive",
	}, nil
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}
