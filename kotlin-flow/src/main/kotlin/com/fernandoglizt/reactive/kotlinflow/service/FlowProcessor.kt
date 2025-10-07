package com.fernandoglizt.reactive.kotlinflow.service

import com.fernandoglizt.reactive.kotlinflow.model.ProcessResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.InetAddress
import java.time.Instant
import kotlin.system.measureTimeMillis

@Service
class FlowProcessor(
    private val webClient: WebClient
) {
    
    private val logger = LoggerFactory.getLogger(FlowProcessor::class.java)
    
    @Value("\${app.flow.batch-concurrency:4}")
    private val batchConcurrency: Int = 4
    
    @Value("\${app.flow.item-concurrency:64}")
    private val itemConcurrency: Int = 64
    
    @Value("\${app.downstream.retry-attempts:1}")
    private val retryAttempts: Int = 1
    
    data class Acc(val processed: Int, val ok: Int, val fail: Int)
    
    fun <T> Flow<T>.chunkedFlow(size: Int): Flow<List<T>> = flow {
        val buffer = ArrayList<T>(size)
        collect { item ->
            buffer.add(item)
            if (buffer.size >= size) {
                emit(ArrayList(buffer))
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) emit(ArrayList(buffer))
    }

    suspend fun processEvents(
        count: Int,
        batch: Int,
        ioDelayMs: Int,
        downstreamUrl: String
    ): ProcessResponse {
        val startTime = Instant.now()
        val node = InetAddress.getLocalHost().hostName
        
        logger.info("Starting flow processing: count={}, batch={}, ioDelayMs={}, downstreamUrl={}", 
            count, batch, ioDelayMs, downstreamUrl)
        
        lateinit var acc: Acc
        val durationMs = measureTimeMillis {
            acc = (1..count)
                .asFlow()
                .map { it * 2 }
                .filter { it % 2 == 0 }
                .flowOn(Dispatchers.Default)
                .chunkedFlow(batch)
                .flatMapMerge(batchConcurrency) { lote ->
                    lote.asFlow().flatMapMerge(itemConcurrency) { item ->
                        flow { emit(callDownstreamWithRetry(item, ioDelayMs, downstreamUrl)) }
                    }
                }
                .fold(Acc(0, 0, 0)) { a, ok ->
                    if (ok) a.copy(processed = a.processed + 1, ok = a.ok + 1)
                    else a.copy(processed = a.processed + 1, fail = a.fail + 1)
                }
        }
        
        val processedEvents = acc.processed
        val externalCallsOk = acc.ok
        val externalCallsFail = acc.fail
        val batches = (processedEvents + batch - 1) / batch
        val eventsPerSec = if (durationMs > 0) processedEvents * 1000.0 / durationMs else 0.0
        
        logger.info("Flow processing completed: processedEvents={}, externalCallsOk={}, externalCallsFail={}, durationMs={}, eventsPerSec={}", 
            processedEvents, externalCallsOk, externalCallsFail, durationMs, eventsPerSec)
        
        return ProcessResponse(
            ok = externalCallsFail == 0,
            count = count,
            batch = batch,
            ioDelayMs = ioDelayMs,
            downstreamUrl = downstreamUrl,
            processedEvents = processedEvents,
            batches = batches,
            externalCallsOk = externalCallsOk,
            externalCallsFail = externalCallsFail,
            durationMs = durationMs,
            eventsPerSec = eventsPerSec,
            timestamp = startTime.toString(),
            node = node
        )
    }
    
    private suspend fun callDownstreamWithRetry(item: Int, ioDelayMs: Int, downstreamUrl: String): Boolean {
        repeat(retryAttempts + 1) { attempt ->
            try {
                val url = appendDelayParam(downstreamUrl, ioDelayMs)
                val ok = webClient.get()
                    .uri(url)
                    .awaitExchange { resp ->
                        val s = resp.statusCode()
                        if (s.is2xxSuccessful) {
                            resp.releaseBody()
                            true
                        } else {
                            resp.releaseBody()
                            false
                        }
                    }
                if (ok) return true
            } catch (e: Exception) {
                if (attempt == retryAttempts) return false
                val backoff = 100L shl attempt
                delay(backoff.coerceAtMost(300))
            }
        }
        return false
    }
    
    private fun appendDelayParam(url: String, delay: Int): String {
        return if ('?' in url) {
            "$url&delay_ms=$delay"
        } else {
            "$url?delay_ms=$delay"
        }
    }
}
