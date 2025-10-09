package com.fernandoglizt.reactive.javareactor.controller;

import com.fernandoglizt.reactive.javareactor.model.ProcessRequest;
import com.fernandoglizt.reactive.javareactor.model.ProcessResponse;
import com.fernandoglizt.reactive.javareactor.service.ReactiveProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(ProcessController.class)
@Import(ProcessControllerTest.TestConfig.class)
class ProcessControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveProcessor reactiveProcessor;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ReactiveProcessor reactiveProcessor() {
            return mock(ReactiveProcessor.class);
        }
    }

    @Test
    void shouldReturn400WhenCountIsZero() {
        ProcessRequest request = new ProcessRequest(0, 10, 50, "http://slow-io:8080/slow");
        
        webTestClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenCountIsNegative() {
        ProcessRequest request = new ProcessRequest(-1, 10, 50, "http://slow-io:8080/slow");
        
        webTestClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenBatchIsZero() {
        ProcessRequest request = new ProcessRequest(100, 0, 50, "http://slow-io:8080/slow");
        
        webTestClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenBatchIsNegative() {
        ProcessRequest request = new ProcessRequest(100, -5, 50, "http://slow-io:8080/slow");
        
        webTestClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenCountExceedsMax() {
        // This test should pass because it hits the validation before the service
        ProcessRequest request = new ProcessRequest(300000, 10, 50, "http://slow-io:8080/slow");
        
        webTestClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnHealthCheck() {
        webTestClient.get()
                .uri("/healthz")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true)
                .jsonPath("$.ts").exists();
    }

    @Test
    void shouldReturnInfo() {
        webTestClient.get()
                .uri("/info")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.name").isEqualTo("java-reactor")
                .jsonPath("$.version").exists()
                .jsonPath("$.description").exists();
    }
}
