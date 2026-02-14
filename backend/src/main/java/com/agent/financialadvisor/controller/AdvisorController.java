package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.RecommendationRepository;
import com.agent.financialadvisor.service.PortfolioRecommendationService;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.agent.financialadvisor.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/advisor")
// CORS handled by WebConfig - no need for @CrossOrigin here
public class AdvisorController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorController.class);
    private final OrchestratorService orchestratorService;
    private final RecommendationRepository recommendationRepository;
    private final PortfolioRecommendationService portfolioRecommendationService;
    private final PortfolioRepository portfolioRepository;

    public AdvisorController(
            OrchestratorService orchestratorService,
            RecommendationRepository recommendationRepository,
            PortfolioRecommendationService portfolioRecommendationService,
            PortfolioRepository portfolioRepository
    ) {
        this.orchestratorService = orchestratorService;
        this.recommendationRepository = recommendationRepository;
        this.portfolioRecommendationService = portfolioRecommendationService;
        this.portfolioRepository = portfolioRepository;
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
            
            // If no recommendations exist, trigger portfolio recommendation generation in background
            if (recommendations.isEmpty()) {
                log.info("No portfolio recommendations found for user {}, triggering background generation", userId);
                portfolioRecommendationService.generatePortfolioRecommendations(userId);
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
            
            log.info("🚀 Triggering portfolio recommendation generation for user: {}", userId);
            
            // First, delete ALL existing recommendations to start fresh
            List<Recommendation> existing = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!existing.isEmpty()) {
                log.info("🗑️ Deleting {} existing recommendations for user {} to start fresh", existing.size(), userId);
                recommendationRepository.deleteAll(existing);
            }
            
            // Trigger generation
            portfolioRecommendationService.generatePortfolioRecommendations(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Portfolio recommendation generation started in background. This may take a few minutes.");
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering recommendation generation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error generating recommendations: " + e.getMessage()));
        }
    }
    
    /**
     * Debug endpoint to check portfolio and recommendations status
     * GET /api/advisor/debug-recommendations
     */
    @GetMapping("/debug-recommendations")
    public ResponseEntity<Map<String, Object>> debugRecommendations() {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            Map<String, Object> debug = new HashMap<>();
            
            // Get portfolio
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isPresent() && portfolioOpt.get().getHoldings() != null) {
                List<String> holdings = portfolioOpt.get().getHoldings().stream()
                    .map(h -> h.getSymbol().toUpperCase())
                    .distinct()
                    .toList();
                debug.put("portfolioHoldings", holdings);
                debug.put("portfolioHoldingsCount", holdings.size());
            } else {
                debug.put("portfolioHoldings", List.of());
                debug.put("portfolioHoldingsCount", 0);
            }
            
            // Get recommendations
            List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            Map<String, List<String>> recommendationsBySymbol = new HashMap<>();
            for (Recommendation rec : recommendations) {
                recommendationsBySymbol.computeIfAbsent(rec.getSymbol(), k -> new ArrayList<>()).add(rec.getAction().toString());
            }
            
            debug.put("recommendationsCount", recommendations.size());
            debug.put("recommendationsBySymbol", recommendationsBySymbol);
            debug.put("recommendations", recommendations.stream()
                .map(r -> Map.of(
                    "id", r.getId(),
                    "symbol", r.getSymbol(),
                    "action", r.getAction().toString(),
                    "createdAt", r.getCreatedAt().toString()
                ))
                .toList());
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error in debug endpoint: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error: " + e.getMessage()));
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

