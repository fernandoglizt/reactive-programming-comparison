package com.fernandoglizt.reactive.javareactor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessRequest {
    
    @JsonProperty("count")
    private int count;
    
    @JsonProperty("batch")
    private int batch;
    
    @JsonProperty("io_delay_ms")
    private int ioDelayMs = 50;
    
    @JsonProperty("downstream_url")
    private String downstreamUrl = "http://slow-io:8080/slow";
    
    // Default constructor
    public ProcessRequest() {}
    
    // Constructor with all fields
    public ProcessRequest(int count, int batch, int ioDelayMs, String downstreamUrl) {
        this.count = count;
        this.batch = batch;
        this.ioDelayMs = ioDelayMs;
        this.downstreamUrl = downstreamUrl;
    }
    
    // Getters and setters
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
}
