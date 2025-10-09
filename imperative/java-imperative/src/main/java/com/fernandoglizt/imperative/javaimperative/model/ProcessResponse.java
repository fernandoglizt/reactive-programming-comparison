package com.fernandoglizt.imperative.javaimperative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ProcessResponse(
    boolean ok,
    int count,
    int batch,
    @JsonProperty("io_delay_ms") int ioDelayMs,
    @JsonProperty("downstream_url") String downstreamUrl,
    @JsonProperty("processed_events") int processedEvents,
    int batches,
    @JsonProperty("external_calls_ok") int externalCallsOk,
    @JsonProperty("external_calls_fail") int externalCallsFail,
    @JsonProperty("duration_ms") long durationMs,
    @JsonProperty("events_per_sec") double eventsPerSec,
    Instant ts,
    String node,
    String notes
) {
    public static ProcessResponse success(
        ProcessRequest request,
        int processedEvents,
        int batches,
        int externalCallsOk,
        int externalCallsFail,
        long durationMs,
        String hostname
    ) {
        double eventsPerSec = durationMs > 0 ? (processedEvents * 1000.0 / durationMs) : 0.0;
        
        return new ProcessResponse(
            true,
            request.count(),
            request.batch(),
            request.getIoDelayMs(),
            request.getDownstreamUrl(),
            processedEvents,
            batches,
            externalCallsOk,
            externalCallsFail,
            durationMs,
            eventsPerSec,
            Instant.now(),
            hostname,
            "imperative blocking with Virtual Threads (Java 21)"
        );
    }
}

