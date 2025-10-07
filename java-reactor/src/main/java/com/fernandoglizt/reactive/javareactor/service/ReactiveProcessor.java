package com.fernandoglizt.reactive.javareactor.service;

import com.fernandoglizt.reactive.javareactor.model.ProcessRequest;
import com.fernandoglizt.reactive.javareactor.model.ProcessResponse;
import com.fasterxml.jackson.databind.JsonNode;
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

@Service
public class ReactiveProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveProcessor.class);

    private final WebClient webClient;
    private final int concurrency;
    private final int retryAttempts;

    public ReactiveProcessor(WebClient webClient,
                           @Value("${app.flatmap.concurrency:64}") int concurrency,
                           @Value("${app.downstream.retry-attempts:1}") int retryAttempts) {
        this.webClient = webClient;
        this.concurrency = concurrency;
        this.retryAttempts = retryAttempts;
    }

    public Mono<ProcessResponse> processEvents(ProcessRequest request) {
        Instant startTime = Instant.now();
        
        return Flux.range(1, request.getCount())
                .map(i -> i * 2) // Transformação leve: multiplicar por 2
                .filter(i -> i % 2 == 0) // Manter apenas pares (todos serão pares após *2)
                .buffer(request.getBatch()) // Agrupar em lotes
                .flatMapSequential(batch -> processBatch(batch, request)
                        .onErrorResume(error -> {
                            logger.warn("Error processing batch: {}", error.getMessage());
                            return Flux.fromIterable(batch).map(value -> 
                                new BatchResult(value, false, error.getMessage()));
                        }), concurrency)
                .collectList()
                .map(results -> {
                    int processedEvents = results.size();
                    int batches = (processedEvents + request.getBatch() - 1) / request.getBatch();
                    int externalCallsOk = (int) results.stream().filter(BatchResult::isSuccess).count();
                    int externalCallsFail = results.size() - externalCallsOk;
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

    private Flux<BatchResult> processBatch(java.util.List<Integer> batch, ProcessRequest request) {
        return Flux.fromIterable(batch)
                .flatMap(value -> callDownstreamService(value, request)
                        .map(success -> new BatchResult(value, success, null))
                        .onErrorResume(error -> 
                            Mono.just(new BatchResult(value, false, error.getMessage())))
                        .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(100))
                                .maxBackoff(Duration.ofMillis(300))));
    }

    private Mono<Boolean> callDownstreamService(Integer value, ProcessRequest request) {
        String url = request.getDownstreamUrl() + "?delay_ms=" + request.getIoDelayMs();
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    boolean ok = response.has("ok") && response.get("ok").asBoolean();
                    int code = response.has("code") ? response.get("code").asInt() : 500;
                    return ok && code >= 200 && code < 300;
                })
                .onErrorReturn(false);
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static class BatchResult {
        private final Integer value;
        private final boolean success;
        private final String error;

        public BatchResult(Integer value, boolean success, String error) {
            this.value = value;
            this.success = success;
            this.error = error;
        }

        public Integer getValue() {
            return value;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }
}
