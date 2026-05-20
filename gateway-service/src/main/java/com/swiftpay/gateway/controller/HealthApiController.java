package com.swiftpay.gateway.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApiController {

    private final HealthEndpoint healthEndpoint;

    public HealthApiController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }


    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthComponent> health() {
        HealthComponent health = healthEndpoint.health();
        if (health.getStatus().getCode().equals("UP")) {
            return ResponseEntity.ok(health);
        }
        return ResponseEntity.status(503).body(health);
    }

    @GetMapping(value = "/health/live", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthComponent> liveness() {
        return ResponseEntity.ok(healthEndpoint.health());
    }

    @GetMapping(value = "/health/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthComponent> readiness() {
        HealthComponent health = healthEndpoint.health();
        if (health.getStatus().getCode().equals("UP")) {
            return ResponseEntity.ok(health);
        }
        return ResponseEntity.status(503).body(health);
    }
}
