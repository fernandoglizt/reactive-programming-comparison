package com.fernandoglizt.reactive.javareactor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ProcessResponse {
    
    @JsonProperty("ok")
    private boolean ok;
    
    @JsonProperty("count")
    private int count;
    
    @JsonProperty("batch")
    private int batch;
    
    @JsonProperty("io_delay_ms")
    private int ioDelayMs;
    
    @JsonProperty("downstream_url")
    private String downstreamUrl;
    
    @JsonProperty("processed_events")
    private int processedEvents;
    
    @JsonProperty("batches")
    private int batches;
    
    @JsonProperty("external_calls_ok")
    private int externalCallsOk;
    
    @JsonProperty("external_calls_fail")
    private int externalCallsFail;
    
    @JsonProperty("duration_ms")
    private long durationMs;
    
    @JsonProperty("events_per_sec")
    private double eventsPerSec;
    
    @JsonProperty("ts")
    private String timestamp;
    
    @JsonProperty("node")
    private String node;
    
    @JsonProperty("notes")
    private String notes;
    
    // Default constructor
    public ProcessResponse() {}
    
    // Constructor with all fields
    public ProcessResponse(boolean ok, int count, int batch, int ioDelayMs, String downstreamUrl,
                          int processedEvents, int batches, int externalCallsOk, int externalCallsFail,
                          long durationMs, double eventsPerSec, String timestamp, String node, String notes) {
        this.ok = ok;
        this.count = count;
        this.batch = batch;
        this.ioDelayMs = ioDelayMs;
        this.downstreamUrl = downstreamUrl;
        this.processedEvents = processedEvents;
        this.batches = batches;
        this.externalCallsOk = externalCallsOk;
        this.externalCallsFail = externalCallsFail;
        this.durationMs = durationMs;
        this.eventsPerSec = eventsPerSec;
        this.timestamp = timestamp;
        this.node = node;
        this.notes = notes;
    }
    
    // Getters and setters
    public boolean isOk() {
        return ok;
    }
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public int getBatch() {
        return batch;
    }
    
    public void setBatch(int batch) {
        this.batch = batch;
    }
    
    public int getIoDelayMs() {
        return ioDelayMs;
    }
    
    public void setIoDelayMs(int ioDelayMs) {
        this.ioDelayMs = ioDelayMs;
    }
    
    public String getDownstreamUrl() {
        return downstreamUrl;
    }
    
    public void setDownstreamUrl(String downstreamUrl) {
        this.downstreamUrl = downstreamUrl;
    }
    
    public int getProcessedEvents() {
        return processedEvents;
    }
    
    public void setProcessedEvents(int processedEvents) {
        this.processedEvents = processedEvents;
    }
    
    public int getBatches() {
        return batches;
    }
    
    public void setBatches(int batches) {
        this.batches = batches;
    }
    
    public int getExternalCallsOk() {
        return externalCallsOk;
    }
    
    public void setExternalCallsOk(int externalCallsOk) {
        this.externalCallsOk = externalCallsOk;
    }
    
    public int getExternalCallsFail() {
        return externalCallsFail;
    }
    
    public void setExternalCallsFail(int externalCallsFail) {
        this.externalCallsFail = externalCallsFail;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public double getEventsPerSec() {
        return eventsPerSec;
    }
    
    public void setEventsPerSec(double eventsPerSec) {
        this.eventsPerSec = eventsPerSec;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getNode() {
        return node;
    }
    
    public void setNode(String node) {
        this.node = node;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
