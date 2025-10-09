package com.fernandoglizt.reactive.kotlinflow.controller

import com.fernandoglizt.reactive.kotlinflow.model.ProcessRequest
import com.fernandoglizt.reactive.kotlinflow.model.ProcessResponse
import com.fernandoglizt.reactive.kotlinflow.service.FlowProcessor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import kotlinx.coroutines.delay
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@WebFluxTest(ProcessController::class)
@ContextConfiguration(classes = [ProcessControllerTest.TestConfig::class])
class ProcessControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var flowProcessor: FlowProcessor

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun webClient(): WebClient = WebClient.builder().build()
    }

    @Test
    fun `should return 200 and process request successfully`() = runBlocking {
        // Given
        val request = ProcessRequest(count = 100, batch = 10, ioDelayMs = 50)
        val response = ProcessResponse(
            ok = true,
            count = 100,
            batch = 10,
            ioDelayMs = 50,
            downstreamUrl = "http://slow-io:8080/slow",
            processedEvents = 100,
            batches = 10,
            externalCallsOk = 100,
            externalCallsFail = 0,
            durationMs = 500,
            eventsPerSec = 200.0,
            timestamp = "2025-10-07T01:00:00Z",
            node = "test-node"
        )

        whenever(flowProcessor.processEvents(any(), any(), any(), any())).thenReturn(response)

        // When & Then
        webTestClient.post()
            .uri("/process")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.ok").isEqualTo(true)
            .jsonPath("$.count").isEqualTo(100)
            .jsonPath("$.processed_events").isEqualTo(100)
    }

    @Test
    fun `should return 400 when count is zero`() {
        // Given
        val request = ProcessRequest(count = 0, batch = 10)

        // When & Then
        webTestClient.post()
            .uri("/process")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("count must be > 0")
    }

    @Test
    fun `should return 400 when batch is zero`() {
        // Given
        val request = ProcessRequest(count = 100, batch = 0)

        // When & Then
        webTestClient.post()
            .uri("/process")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("batch must be > 0")
    }

    @Test
    fun `should return 200 for health check`() {
        webTestClient.get()
            .uri("/healthz")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.ok").isEqualTo(true)
    }

    @Test
    fun `should return 200 for info endpoint`() {
        webTestClient.get()
            .uri("/info")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("kotlin-flow")
            .jsonPath("$.version").isEqualTo("0.0.1-SNAPSHOT")
    }
}
