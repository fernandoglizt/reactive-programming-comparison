package com.fernandoglizt.reactive.kotlinflow.controller

import com.fernandoglizt.reactive.kotlinflow.model.ProcessRequest
import com.fernandoglizt.reactive.kotlinflow.service.FlowProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/")
class ProcessController(
    private val flowProcessor: FlowProcessor
) {
    
    private val logger = LoggerFactory.getLogger(ProcessController::class.java)
    
    @Value("\${app.max-count:200000}")
    private val maxCount: Int = 200000

    @PostMapping("/process")
    suspend fun process(@RequestBody request: ProcessRequest): ResponseEntity<Any> {
        logger.info("Processing request: count={}, batch={}, ioDelayMs={}, downstreamUrl={}", 
            request.count, request.batch, request.ioDelayMs, request.downstreamUrl)
        
        if (request.count <= 0) {
            logger.warn("Invalid count: {}", request.count)
            return ResponseEntity.badRequest().body(mapOf("error" to "count must be > 0"))
        }
        
        if (request.batch <= 0) {
            logger.warn("Invalid batch: {}", request.batch)
            return ResponseEntity.badRequest().body(mapOf("error" to "batch must be > 0"))
        }
        
        if (request.count > maxCount) {
            logger.warn("Count exceeds max: {} > {}", request.count, maxCount)
            return ResponseEntity.badRequest().body(mapOf("error" to "count exceeds maximum allowed: $maxCount"))
        }
        
        val normalizedIoDelayMs = if (request.ioDelayMs < 0) 0 else request.ioDelayMs
        
        val response = flowProcessor.processEvents(
            count = request.count,
            batch = request.batch,
            ioDelayMs = normalizedIoDelayMs,
            downstreamUrl = request.downstreamUrl
        )
        
        logger.info("Process completed: processedEvents={}, externalCallsOk={}, externalCallsFail={}, durationMs={}, eventsPerSec={}", 
            response.processedEvents, response.externalCallsOk, response.externalCallsFail, 
            response.durationMs, response.eventsPerSec)
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/healthz")
    suspend fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "ok" to true,
            "ts" to java.time.Instant.now().toString()
        ))
    }
    
    @GetMapping("/info")
    suspend fun info(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "name" to "kotlin-flow",
            "version" to "0.0.1-SNAPSHOT",
            "javaVersion" to System.getProperty("java.version"),
            "buildTime" to java.time.Instant.now().toString(),
            "description" to "Reactive Spring Boot application with Kotlin Flow and Coroutines",
            "maxCount" to maxCount
        ))
    }
}
