package com.fernandoglizt.imperative.kotlinimperative.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class HttpClientConfig {

    @Value("\${app.downstream.timeout-ms:2000}")
    private val timeoutMs: Int = 2000

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofMillis(timeoutMs.toLong()))
            .setReadTimeout(Duration.ofMillis(timeoutMs.toLong()))
            .build()
    }
}

