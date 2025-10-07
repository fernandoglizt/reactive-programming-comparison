package com.fernandoglizt.reactive.kotlinflow.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ProcessResponse(
    @JsonProperty("ok")
    val ok: Boolean,
    
    @JsonProperty("count")
    val count: Int,
    
    @JsonProperty("batch")
    val batch: Int,
    
    @JsonProperty("io_delay_ms")
    val ioDelayMs: Int,
    
    @JsonProperty("downstream_url")
    val downstreamUrl: String,
    
    @JsonProperty("processed_events")
    val processedEvents: Int,
    
    @JsonProperty("batches")
    val batches: Int,
    
    @JsonProperty("external_calls_ok")
    val externalCallsOk: Int,
    
    @JsonProperty("external_calls_fail")
    val externalCallsFail: Int,
    
    @JsonProperty("duration_ms")
    val durationMs: Long,
    
    @JsonProperty("events_per_sec")
    val eventsPerSec: Double,
    
    @JsonProperty("ts")
    val timestamp: String,
    
    @JsonProperty("node")
    val node: String,
    
    @JsonProperty("notes")
    val notes: String = "kotlin flow non-blocking"
)
