package com.fernandoglizt.reactive.kotlinflow.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient

@SpringBootTest
@TestPropertySource(properties = [
    "app.flow.concurrency=2",
    "app.downstream.retry-attempts=1"
])
class FlowProcessorTest {

    @Autowired
    private lateinit var flowProcessor: FlowProcessor

    @Test
    fun `should create FlowProcessor`() {
        // This test verifies that FlowProcessor can be instantiated
        // and the Spring context loads correctly
        assert(flowProcessor != null)
    }
}
