package com.fernandoglizt.imperative.javaimperative.service;

import com.fernandoglizt.imperative.javaimperative.model.ProcessRequest;
import com.fernandoglizt.imperative.javaimperative.model.ProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
public class ImperativeProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImperativeProcessor.class);

    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    
    @Value("${app.downstream.retry-attempts:1}")
    private int retryAttempts;

    public ImperativeProcessor(RestTemplate restTemplate, ExecutorService executorService) {
        this.restTemplate = restTemplate;
        this.executorService = executorService;
    }

    public ProcessResponse processEvents(ProcessRequest request) {
        long startTime = System.currentTimeMillis();
        
        List<Integer> items = IntStream.rangeClosed(1, request.count())
            .map(i -> i * 2)
            .filter(i -> i % 2 == 0)
            .boxed()
            .toList();

        AtomicInteger externalCallsOk = new AtomicInteger(0);
        AtomicInteger externalCallsFail = new AtomicInteger(0);
        
        List<List<Integer>> batches = createBatches(items, request.batch());
        int batchCount = batches.size();
        
        CountDownLatch latch = new CountDownLatch(items.size());
        
        for (List<Integer> batch : batches) {
            for (Integer item : batch) {
                executorService.submit(() -> {
                    try {
                        boolean success = callDownstreamWithRetry(
                            item, 
                            request.getIoDelayMs(), 
                            request.getDownstreamUrl()
                        );
                        if (success) {
                            externalCallsOk.incrementAndGet();
                        } else {
                            externalCallsFail.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted", e);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        String hostname = getHostname();

        ProcessResponse response = ProcessResponse.success(
            request,
            items.size(),
            batchCount,
            externalCallsOk.get(),
            externalCallsFail.get(),
            durationMs,
            hostname
        );

        log.info("Processed {} events in {}ms ({} events/sec) - OK: {}, Fail: {}",
            items.size(), durationMs, response.eventsPerSec(),
            externalCallsOk.get(), externalCallsFail.get());

        return response;
    }

    private boolean callDownstreamWithRetry(int item, int delayMs, String baseUrl) {
        String url = appendDelayParam(baseUrl, delayMs);
        
        for (int attempt = 0; attempt <= retryAttempts; attempt++) {
            try {
                restTemplate.getForObject(url, String.class);
                return true;
            } catch (Exception e) {
                if (attempt < retryAttempts) {
                    try {
                        long backoff = Math.min(100L * (1L << attempt), 300L);
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private String appendDelayParam(String url, int delay) {
        return url.contains("?") 
            ? url + "&delay_ms=" + delay 
            : url + "?delay_ms=" + delay;
    }

    private List<List<Integer>> createBatches(List<Integer> items, int batchSize) {
        List<List<Integer>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

