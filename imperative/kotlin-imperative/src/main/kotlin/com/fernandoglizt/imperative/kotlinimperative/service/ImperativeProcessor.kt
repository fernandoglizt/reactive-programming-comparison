package com.fernandoglizt.imperative.kotlinimperative.service

import com.fernandoglizt.imperative.kotlinimperative.model.ProcessRequest
import com.fernandoglizt.imperative.kotlinimperative.model.ProcessResponse
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

@Service
class ImperativeProcessor(
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${app.downstream.retry-attempts:1}")
    private val retryAttempts: Int = 1

    @Value("\${app.concurrency:128}")
    private val concurrency: Int = 128

    suspend fun processEvents(request: ProcessRequest): ProcessResponse = coroutineScope {
        val startTime = System.currentTimeMillis()

        val items = (1..request.count)
            .map { it * 2 }
            .filter { it % 2 == 0 }

        val externalCallsOk = AtomicInteger(0)
        val externalCallsFail = AtomicInteger(0)

        val batches = items.chunked(request.batch)
        val batchCount = batches.size

        // Process all items with controlled concurrency
        items.chunked(concurrency).forEach { chunk ->
            val jobs = chunk.map { item ->
                async(Dispatchers.IO) {
                    val success = callDownstreamWithRetry(
                        item,
                        request.getIoDelayMsNormalized(),
                        request.getDownstreamUrlOrDefault()
                    )
                    if (success) {
                        externalCallsOk.incrementAndGet()
                    } else {
                        externalCallsFail.incrementAndGet()
                    }
                }
            }
            jobs.awaitAll()
        }

        val durationMs = System.currentTimeMillis() - startTime
        val hostname = getHostname()

        val response = ProcessResponse.success(
            request = request,
            processedEvents = items.size,
            batches = batchCount,
            externalCallsOk = externalCallsOk.get(),
            externalCallsFail = externalCallsFail.get(),
            durationMs = durationMs,
            hostname = hostname
        )

        log.info(
            "Processed {} events in {}ms ({} events/sec) - OK: {}, Fail: {}",
            items.size, durationMs, response.eventsPerSec,
            externalCallsOk.get(), externalCallsFail.get()
        )

        response
    }

    private suspend fun callDownstreamWithRetry(item: Int, delayMs: Int, baseUrl: String): Boolean {
        val url = appendDelayParam(baseUrl, delayMs)

        repeat(retryAttempts + 1) { attempt ->
            try {
                // Call blocking RestTemplate from IO dispatcher
                withContext(Dispatchers.IO) {
                    restTemplate.getForObject(url, String::class.java)
                }
                return true
            } catch (e: Exception) {
                if (attempt < retryAttempts) {
                    val backoff = minOf(100L * (1L shl attempt), 300L)
                    delay(backoff)
                }
            }
        }
        return false
    }

    private fun appendDelayParam(url: String, delay: Int): String {
        return if ('?' in url) "$url&delay_ms=$delay" else "$url?delay_ms=$delay"
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }
}

