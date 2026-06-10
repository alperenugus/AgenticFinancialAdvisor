package com.agent.financialadvisor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public liveness endpoint used by the Railway healthcheck and Docker HEALTHCHECK.
 *
 * It MUST remain unauthenticated (see SecurityConfig.permitAll for "/api/health"). If a healthcheck
 * path is protected by Spring Security it 302-redirects to the OAuth login page, Railway interprets
 * that as "service unavailable", and the deploy is marked FAILED while an older build keeps serving.
 * This is a pure liveness check — it does not touch the database or any external API on purpose, so a
 * slow/cold dependency can never flap the deploy.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "financial-advisor",
                "timestamp", Instant.now().toString()
        ));
    }
}
