package com.fernandoglizt.imperative.javaimperative.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpClientConfig {

    @Value("${app.downstream.timeout-ms:2000}")
    private int timeoutMs;

    @Value("${app.executor.pool-size:128}")
    private int poolSize;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofMillis(timeoutMs))
            .setReadTimeout(Duration.ofMillis(timeoutMs))
            .build();
    }

    @Bean
    public ExecutorService executorService() {
        // Use Virtual Threads (Java 21+) for better scalability
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

