package com.fernandoglizt.imperative.javaimperative.controller;

import com.fernandoglizt.imperative.javaimperative.model.ProcessRequest;
import com.fernandoglizt.imperative.javaimperative.model.ProcessResponse;
import com.fernandoglizt.imperative.javaimperative.service.ImperativeProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;

@RestController
public class ProcessController {

    private final ImperativeProcessor processor;

    @Value("${app.max-count:200000}")
    private int maxCount;

    @Value("${app.version:1.0.0}")
    private String version;

    public ProcessController(ImperativeProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestBody ProcessRequest request) {
        if (request.count() > maxCount) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "count exceeds maximum allowed",
                    "max_count", maxCount,
                    "requested", request.count()
                ));
        }

        try {
            ProcessResponse response = processor.processEvents(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "ok", true,
            "ts", Instant.now()
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        String hostname = getHostname();
        String javaVersion = System.getProperty("java.version");
        
        return ResponseEntity.ok(Map.of(
            "name", "java-imperative",
            "version", version,
            "javaVersion", javaVersion,
            "maxCount", maxCount,
            "node", hostname,
            "notes", "Imperative blocking implementation with ExecutorService"
        ));
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

