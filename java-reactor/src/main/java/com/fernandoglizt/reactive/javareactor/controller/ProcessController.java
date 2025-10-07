package com.fernandoglizt.reactive.javareactor.controller;

import com.fernandoglizt.reactive.javareactor.model.ProcessRequest;
import com.fernandoglizt.reactive.javareactor.model.ProcessResponse;
import com.fernandoglizt.reactive.javareactor.service.ReactiveProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/")
public class ProcessController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);

    private final ReactiveProcessor reactiveProcessor;
    private final int maxCount;

    public ProcessController(ReactiveProcessor reactiveProcessor,
                           @Value("${app.max-count:200000}") int maxCount) {
        this.reactiveProcessor = reactiveProcessor;
        this.maxCount = maxCount;
    }

    @PostMapping("/process")
    public Mono<ResponseEntity<ProcessResponse>> process(@RequestBody ProcessRequest request) {
        logger.info("Processing request: count={}, batch={}, ioDelayMs={}, downstreamUrl={}", 
                request.getCount(), request.getBatch(), request.getIoDelayMs(), request.getDownstreamUrl());

        // Validações
        if (request.getCount() <= 0) {
            logger.warn("Invalid count: {}", request.getCount());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        if (request.getBatch() <= 0) {
            logger.warn("Invalid batch: {}", request.getBatch());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        if (request.getCount() > maxCount) {
            logger.warn("Count exceeds maximum: {} > {}", request.getCount(), maxCount);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Normalizar ioDelayMs se negativo
        if (request.getIoDelayMs() < 0) {
            request.setIoDelayMs(0);
        }

        return reactiveProcessor.processEvents(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error processing events: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/healthz")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "ok", true,
                "ts", Instant.now().toString()
        )));
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "name", "java-reactor",
                "version", "0.0.1-SNAPSHOT",
                "description", "Reactive Spring Boot application with WebFlux and Project Reactor",
                "buildTime", Instant.now().toString(),
                "javaVersion", System.getProperty("java.version"),
                "maxCount", maxCount
        )));
    }
}
