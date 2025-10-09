package com.fernandoglizt.imperative.kotlinimperative.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ProcessResponse(
    val ok: Boolean,
    val count: Int,
    val batch: Int,
    @JsonProperty("io_delay_ms") val ioDelayMs: Int,
    @JsonProperty("downstream_url") val downstreamUrl: String,
    @JsonProperty("processed_events") val processedEvents: Int,
    val batches: Int,
    @JsonProperty("external_calls_ok") val externalCallsOk: Int,
    @JsonProperty("external_calls_fail") val externalCallsFail: Int,
    @JsonProperty("duration_ms") val durationMs: Long,
    @JsonProperty("events_per_sec") val eventsPerSec: Double,
    val ts: Instant,
    val node: String,
    val notes: String
) {
    companion object {
        fun success(
            request: ProcessRequest,
            processedEvents: Int,
            batches: Int,
            externalCallsOk: Int,
            externalCallsFail: Int,
            durationMs: Long,
            hostname: String
        ): ProcessResponse {
            val eventsPerSec = if (durationMs > 0) processedEvents * 1000.0 / durationMs else 0.0
            
            return ProcessResponse(
                ok = true,
                count = request.count,
                batch = request.batch,
                ioDelayMs = request.getIoDelayMsNormalized(),
                downstreamUrl = request.getDownstreamUrlOrDefault(),
                processedEvents = processedEvents,
                batches = batches,
                externalCallsOk = externalCallsOk,
                externalCallsFail = externalCallsFail,
                durationMs = durationMs,
                eventsPerSec = eventsPerSec,
                ts = Instant.now(),
                node = hostname,
                notes = "imperative blocking with Kotlin coroutines async/await"
            )
        }
    }
}

