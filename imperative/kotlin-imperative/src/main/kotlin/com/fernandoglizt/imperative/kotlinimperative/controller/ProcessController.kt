package com.fernandoglizt.imperative.kotlinimperative.controller

import com.fernandoglizt.imperative.kotlinimperative.model.ProcessRequest
import com.fernandoglizt.imperative.kotlinimperative.model.ProcessResponse
import com.fernandoglizt.imperative.kotlinimperative.service.ImperativeProcessor
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.InetAddress
import java.time.Instant

@RestController
class ProcessController(
    private val processor: ImperativeProcessor
) {

    @Value("\${app.max-count:200000}")
    private val maxCount: Int = 200000

    @Value("\${app.version:1.0.0}")
    private val version: String = "1.0.0"

    @PostMapping("/process")
    fun process(@RequestBody request: ProcessRequest): ResponseEntity<*> = runBlocking {
        if (request.count > maxCount) {
            return@runBlocking ResponseEntity.badRequest().body(
                mapOf(
                    "error" to "count exceeds maximum allowed",
                    "max_count" to maxCount,
                    "requested" to request.count
                )
            )
        }

        try {
            val response = processor.processEvents(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Internal server error: ${e.message}"))
        }
    }

    @GetMapping("/healthz")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "ok" to true,
                "ts" to Instant.now()
            )
        )
    }

    @GetMapping("/info")
    fun info(): ResponseEntity<Map<String, Any>> {
        val hostname = getHostname()
        val kotlinVersion = KotlinVersion.CURRENT.toString()

        return ResponseEntity.ok(
            mapOf(
                "name" to "kotlin-imperative",
                "version" to version,
                "kotlinVersion" to kotlinVersion,
                "maxCount" to maxCount,
                "node" to hostname,
                "notes" to "Imperative blocking with Kotlin coroutines async/await"
            )
        )
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }
}

