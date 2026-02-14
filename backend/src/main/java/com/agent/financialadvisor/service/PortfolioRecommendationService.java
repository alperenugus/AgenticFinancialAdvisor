package com.agent.financialadvisor.service;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.RecommendationRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.agent.financialadvisor.service.agents.StockDiscoveryAgent;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service to generate portfolio-level recommendations using AI agents
 * Uses real-time stock discovery instead of hardcoded lists
 */
@Service
public class PortfolioRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRecommendationService.class);
    
    private final UserProfileRepository userProfileRepository;
    private final PortfolioRepository portfolioRepository;
    private final RecommendationRepository recommendationRepository;
    private final StockDiscoveryAgent stockDiscoveryAgent;
    private final OrchestratorService orchestratorService;
    private final ObjectMapper objectMapper;

    public PortfolioRecommendationService(
            UserProfileRepository userProfileRepository,
            PortfolioRepository portfolioRepository,
            RecommendationRepository recommendationRepository,
            StockDiscoveryAgent stockDiscoveryAgent,
            OrchestratorService orchestratorService,
            ObjectMapper objectMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.portfolioRepository = portfolioRepository;
        this.recommendationRepository = recommendationRepository;
        this.stockDiscoveryAgent = stockDiscoveryAgent;
        this.orchestratorService = orchestratorService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate portfolio recommendations for a user based on their profile and current portfolio
     * This uses the orchestrator to discover stocks and make recommendations
     */
    @Async
    public CompletableFuture<Void> generatePortfolioRecommendations(String userId) {
        log.info("Generating portfolio recommendations for user: {}", userId);
        
        try {
            // Get user profile
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                log.info("No profile found for user {}, skipping portfolio recommendation generation", userId);
                return CompletableFuture.completedFuture(null);
            }

            UserProfile profile = profileOpt.get();
            
            // Get portfolio to see what they already own
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            List<String> ownedSymbols = new ArrayList<>();
            if (portfolioOpt.isPresent() && portfolioOpt.get().getHoldings() != null) {
                ownedSymbols = portfolioOpt.get().getHoldings().stream()
                    .map(h -> h.getSymbol().toUpperCase())
                    .toList();
            }

            // Build exclude list for stock discovery
            String excludeOwned = String.join(",", ownedSymbols);
            
            // Use StockDiscoveryAgent to find stocks matching user's risk tolerance
            String discoveryResult = stockDiscoveryAgent.discoverStocks(
                profile.getRiskTolerance().toString(),
                profile.getPreferredSectors() != null ? String.join(",", profile.getPreferredSectors()) : "",
                excludeOwned
            );

            // Parse discovered stocks from JSON response
            List<String> discoveredStocks = parseDiscoveredStocks(discoveryResult);
            
            if (discoveredStocks.isEmpty()) {
                log.warn("No stocks discovered for user {}", userId);
                return CompletableFuture.completedFuture(null);
            }

            // Limit to 5 recommendations
            int maxRecommendations = 5;
            int generated = 0;
            String sessionId = "portfolio-recommendation-" + userId + "-" + System.currentTimeMillis();

            // Use orchestrator to generate recommendations for each discovered stock
            for (String symbol : discoveredStocks) {
                if (generated >= maxRecommendations) break;
                
                // Check if recommendation already exists and is recent (within last 7 days)
                Optional<Recommendation> existing = recommendationRepository.findByUserIdAndSymbol(userId, symbol);
                if (existing.isPresent()) {
                    long daysSinceCreation = java.time.Duration.between(
                        existing.get().getCreatedAt(),
                        java.time.LocalDateTime.now()
                    ).toDays();
                    if (daysSinceCreation < 7) {
                        log.debug("Recent recommendation exists for {} and user {}, skipping", symbol, userId);
                        continue;
                    }
                }

                try {
                    // Use orchestrator to analyze and recommend
                    String query = String.format(
                        "Analyze %s for portfolio recommendation. User has risk tolerance: %s, " +
                        "investment horizon: %s, goals: %s. Current portfolio contains: %s. " +
                        "Should this stock be added to the portfolio? Provide a BUY/SELL/HOLD recommendation " +
                        "with detailed reasoning based on portfolio diversification, risk alignment, and market analysis.",
                        symbol,
                        profile.getRiskTolerance(),
                        profile.getHorizon(),
                        profile.getGoals() != null ? profile.getGoals().toString() : "[]",
                        ownedSymbols.isEmpty() ? "no holdings" : String.join(", ", ownedSymbols)
                    );

                    // Use orchestrator to generate recommendation
                    String recommendationText = orchestratorService.coordinateAnalysis(userId, query, sessionId + "-" + symbol);
                    
                    // Parse recommendation from orchestrator response and save
                    saveRecommendationFromOrchestrator(userId, symbol, recommendationText, profile);

                    generated++;
                    log.info("Generated portfolio recommendation for {}: {}", symbol, recommendationText.substring(0, Math.min(100, recommendationText.length())));

                    // Add delay to avoid rate limiting
                    Thread.sleep(2000);
                } catch (Exception e) {
                    log.warn("Error generating portfolio recommendation for {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Generated {} portfolio recommendations for user {}", generated, userId);
        } catch (Exception e) {
            log.error("Error generating portfolio recommendations for user {}: {}", userId, e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Parse discovered stocks from JSON response
     */
    private List<String> parseDiscoveredStocks(String discoveryResult) {
        List<String> stocks = new ArrayList<>();
        try {
            JsonNode json = objectMapper.readTree(discoveryResult);
            if (json.has("discoveredStocks") && json.get("discoveredStocks").isArray()) {
                for (JsonNode stock : json.get("discoveredStocks")) {
                    stocks.add(stock.asText());
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing discovered stocks: {}", e.getMessage());
        }
        return stocks;
    }

    /**
     * Save recommendation from orchestrator response
     * This is a simplified version - in production, you might want to parse the response more carefully
     */
    private void saveRecommendationFromOrchestrator(String userId, String symbol, String recommendationText, UserProfile profile) {
        try {
            Recommendation recommendation = new Recommendation();
            recommendation.setUserId(userId);
            recommendation.setSymbol(symbol);
            
            // Parse action from text (simplified - could be improved with better parsing)
            if (recommendationText.toUpperCase().contains("BUY")) {
                recommendation.setAction(Recommendation.RecommendationAction.BUY);
            } else if (recommendationText.toUpperCase().contains("SELL")) {
                recommendation.setAction(Recommendation.RecommendationAction.SELL);
            } else {
                recommendation.setAction(Recommendation.RecommendationAction.HOLD);
            }
            
            // Set defaults based on profile
            recommendation.setConfidence(0.7);
            recommendation.setRiskLevel(profile.getRiskTolerance() == UserProfile.RiskTolerance.CONSERVATIVE 
                ? Recommendation.RiskLevel.LOW 
                : profile.getRiskTolerance() == UserProfile.RiskTolerance.AGGRESSIVE 
                    ? Recommendation.RiskLevel.HIGH 
                    : Recommendation.RiskLevel.MEDIUM);
            recommendation.setReasoning(recommendationText);
            recommendation.setTimeHorizon(profile.getHorizon() == UserProfile.InvestmentHorizon.SHORT
                ? Recommendation.InvestmentHorizon.SHORT
                : profile.getHorizon() == UserProfile.InvestmentHorizon.LONG
                    ? Recommendation.InvestmentHorizon.LONG
                    : Recommendation.InvestmentHorizon.MEDIUM);
            
            recommendationRepository.save(recommendation);
        } catch (Exception e) {
            log.error("Error saving recommendation for {}: {}", symbol, e.getMessage());
        }
    }
}

