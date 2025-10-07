package com.fernandoglizt.reactive.javareactor.service;

import com.fernandoglizt.reactive.javareactor.model.ProcessRequest;
import com.fernandoglizt.reactive.javareactor.model.ProcessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ReactiveProcessorTest {

    @Test
    void shouldCreateReactiveProcessor() {
        // Simple test to verify the service can be instantiated
        WebClient webClient = WebClient.builder().build();
        ReactiveProcessor processor = new ReactiveProcessor(webClient, 64, 1);
        
        // Verify it's not null
        assert processor != null;
    }
}
