package com.fernandoglizt.imperative.javaimperative.model;

public record ProcessRequest(
    int count,
    int batch,
    int ioDelayMs,
    String downstreamUrl
) {
    public ProcessRequest {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        if (batch <= 0) {
            throw new IllegalArgumentException("batch must be > 0");
        }
    }

    public String getDownstreamUrl() {
        return downstreamUrl != null && !downstreamUrl.isBlank() 
            ? downstreamUrl 
            : "http://slow-io:8080/slow";
    }

    public int getIoDelayMs() {
        return Math.max(0, ioDelayMs);
    }
}

