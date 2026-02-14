package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.repository.RecommendationRepository;
import com.agent.financialadvisor.service.RecommendationGenerationService;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.agent.financialadvisor.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/advisor")
// CORS handled by WebConfig - no need for @CrossOrigin here
public class AdvisorController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorController.class);
    private final OrchestratorService orchestratorService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationGenerationService recommendationGenerationService;

    public AdvisorController(
            OrchestratorService orchestratorService,
            RecommendationRepository recommendationRepository,
            RecommendationGenerationService recommendationGenerationService
    ) {
        this.orchestratorService = orchestratorService;
        this.recommendationRepository = recommendationRepository;
        this.recommendationGenerationService = recommendationGenerationService;
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

            log.info("Received analysis request: userId={}, query={}", userId, query);

            // Coordinate analysis through orchestrator
            String response = orchestratorService.coordinateAnalysis(userId, query, sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("userId", userId);
            result.put("response", response);
            result.put("status", "success");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in analyze endpoint: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error processing request: " + e.getMessage()));
        }
    }

    /**
     * Get all recommendations for authenticated user
     * GET /api/advisor/recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<Recommendation>> getRecommendations() {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            List<Recommendation> recommendations = recommendationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);
            
            // If no recommendations exist, trigger generation in background
            if (recommendations.isEmpty()) {
                log.info("No recommendations found for user {}, triggering background generation", userId);
                recommendationGenerationService.generateRecommendationsForUser(userId);
            }
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate recommendations for authenticated user
     * POST /api/advisor/generate-recommendations
     */
    @PostMapping("/generate-recommendations")
    public ResponseEntity<Map<String, Object>> generateRecommendations() {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            log.info("Triggering recommendation generation for user: {}", userId);
            recommendationGenerationService.generateRecommendationsForUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Recommendation generation started in background");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering recommendation generation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error generating recommendations: " + e.getMessage()));
        }
    }

    /**
     * Get specific recommendation
     * GET /api/advisor/recommendations/{symbol}
     */
    @GetMapping("/recommendations/{symbol}")
    public ResponseEntity<Recommendation> getRecommendation(@PathVariable String symbol) {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            return recommendationRepository.findByUserIdAndSymbol(userId, symbol)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting recommendation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
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

