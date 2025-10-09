package com.fernandoglizt.reactive.javareactor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.max-count=100",
    "app.flatmap.concurrency=4",
    "app.downstream.timeout-ms=1000",
    "app.downstream.retry-attempts=0"
})
class JavaReactorApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures the Spring context loads successfully
    }
}