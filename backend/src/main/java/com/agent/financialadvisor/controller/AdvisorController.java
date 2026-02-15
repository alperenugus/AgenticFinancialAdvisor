package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.exception.RateLimitExceededException;
import com.agent.financialadvisor.service.RateLimitService;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.agent.financialadvisor.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/advisor")
// CORS handled by WebConfig - no need for @CrossOrigin here
public class AdvisorController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorController.class);
    private final OrchestratorService orchestratorService;
    private final RateLimitService rateLimitService;

    public AdvisorController(
            OrchestratorService orchestratorService,
            RateLimitService rateLimitService
    ) {
        this.orchestratorService = orchestratorService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Main endpoint for getting financial advice
     * POST /api/advisor/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestBody Map<String, String> request
    ) {
        try {
            // Get authenticated user ID
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            String query = request.get("query");
            String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());

            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Query is required"));
            }

            // Check rate limit before processing
            rateLimitService.checkAdvisorRateLimit(sessionId);

            log.info("Received analysis request: userId={}, query={}", userId, query);

            // Coordinate analysis through orchestrator
            String response = orchestratorService.coordinateAnalysis(userId, query, sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("userId", userId);
            result.put("response", response);
            result.put("status", "success");

            // Add rate limit headers to successful response
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingAdvisorTokens(sessionId)));

            return ResponseEntity.ok().headers(headers).body(result);
        } catch (RateLimitExceededException e) {
            // Return 429 Too Many Requests with proper headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-RateLimit-Remaining", String.valueOf(e.getRemainingTokens()));
            headers.add("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            headers.add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + e.getRetryAfterSeconds()));
            
            log.warn("Rate limit exceeded for session: {}", request.getOrDefault("sessionId", "unknown"));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error in analyze endpoint: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error processing request: " + e.getMessage()));
        }
    }

    /**
     * Check agent status
     * GET /api/advisor/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agents", orchestratorService.getAgentStatus());
            status.put("status", "operational");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error getting status"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}

