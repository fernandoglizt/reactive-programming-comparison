package com.fernandoglizt.reactive.kotlinflow

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "app.flow.concurrency=2",
    "app.downstream.retry-attempts=1"
])
class KotlinFlowApplicationTests {

	@Test
	fun contextLoads() {
		// This test verifies that the Spring Boot application context loads correctly
	}

}
