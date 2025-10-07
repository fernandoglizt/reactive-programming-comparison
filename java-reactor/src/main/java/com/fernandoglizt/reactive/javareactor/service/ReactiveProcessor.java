package com.fernandoglizt.reactive.javareactor.service;

import com.fernandoglizt.reactive.javareactor.model.ProcessRequest;
import com.fernandoglizt.reactive.javareactor.model.ProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReactiveProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveProcessor.class);

    private final WebClient webClient;
    private final int batchConcurrency;
    private final int itemConcurrency;
    private final int retryAttempts;

    public ReactiveProcessor(
            WebClient webClient,
            @Value("${app.flatmap.batch-concurrency:4}") int batchConcurrency,
            @Value("${app.flatmap.item-concurrency:64}") int itemConcurrency,
            @Value("${app.downstream.retry-attempts:1}") int retryAttempts
    ) {
        this.webClient = webClient;
        this.batchConcurrency = batchConcurrency;
        this.itemConcurrency = itemConcurrency;
        this.retryAttempts = retryAttempts;
    }

    private static final class Acc {
        final int processed;
        final int ok;
        final int fail;
        Acc(int processed, int ok, int fail) { this.processed = processed; this.ok = ok; this.fail = fail; }
        Acc incOk()   { return new Acc(processed + 1, ok + 1, fail); }
        Acc incFail() { return new Acc(processed + 1, ok, fail + 1); }
    }

    public Mono<ProcessResponse> processEvents(ProcessRequest request) {
        Instant startTime = Instant.now();

        return Flux.range(1, request.getCount())
                .map(i -> i * 2) // Transformação leve: multiplicar por 2
                .filter(i -> i % 2 == 0) // Manter apenas pares (todos serão pares após *2)
                .buffer(request.getBatch()) // Agrupar em lotes
                .flatMap(lote -> processBatch(lote, request), batchConcurrency)
                .reduce(new Acc(0, 0, 0), (acc, ok) -> ok ? acc.incOk() : acc.incFail())
                .map(acc -> {
                    int processedEvents = acc.processed;
                    int externalCallsOk = acc.ok;
                    int externalCallsFail = acc.fail;
                    int batches = (processedEvents + request.getBatch() - 1) / request.getBatch();
                    long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                    double eventsPerSec = durationMs > 0 ? (processedEvents * 1000.0) / durationMs : 0.0;

                    ProcessResponse response = new ProcessResponse(
                            externalCallsFail == 0,
                            request.getCount(),
                            request.getBatch(),
                            request.getIoDelayMs(),
                            request.getDownstreamUrl(),
                            processedEvents,
                            batches,
                            externalCallsOk,
                            externalCallsFail,
                            durationMs,
                            eventsPerSec,
                            Instant.now().toString(),
                            getHostname(),
                            "webflux non-blocking"
                    );

                    logger.info("Process completed: processedEvents={}, externalCallsOk={}, externalCallsFail={}, durationMs={}, eventsPerSec={}",
                            processedEvents, externalCallsOk, externalCallsFail, durationMs, eventsPerSec);

                    return response;
                });
    }

    private Flux<Boolean> processBatch(List<Integer> batch, ProcessRequest request) {
        return Flux.fromIterable(batch)
                .flatMap(value -> callDownstreamService(value, request)
                        .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(100)).maxBackoff(Duration.ofMillis(300))),
                        itemConcurrency
                );
    }

    private Mono<Boolean> callDownstreamService(Integer value, ProcessRequest request) {
        String url = appendDelayParam(request.getDownstreamUrl(), request.getIoDelayMs());

        return webClient.get()
                .uri(url)
                .exchangeToMono(resp -> resp.releaseBody().thenReturn(resp.statusCode().is2xxSuccessful()));
    }

    private String appendDelayParam(String url, int delay) {
        if (url.contains("?")) {
            return url + "&delay_ms=" + delay;
        } else {
            return url + "?delay_ms=" + delay;
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
