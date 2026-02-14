package com.agent.financialadvisor.service;

import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.RecommendationRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.agent.financialadvisor.service.agents.MarketAnalysisAgent;
// import com.agent.financialadvisor.service.agents.RecommendationAgent; // Removed - agent architecture simplified
// import com.agent.financialadvisor.service.agents.ResearchAgent; // Removed - agent architecture simplified
// import com.agent.financialadvisor.service.agents.RiskAssessmentAgent; // Removed - agent architecture simplified
import com.agent.financialadvisor.service.agents.UserProfileAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service to pre-generate recommendations based on user profile and portfolio
 * @deprecated Use PortfolioRecommendationService instead for portfolio-focused recommendations
 */
@Deprecated
@Service
public class RecommendationGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationGenerationService.class);
    
    private final UserProfileRepository userProfileRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserProfileAgent userProfileAgent;
    private final MarketAnalysisAgent marketAnalysisAgent;
    // private final RiskAssessmentAgent riskAssessmentAgent; // Removed - agent architecture simplified
    // private final ResearchAgent researchAgent; // Removed - agent architecture simplified
    // private final RecommendationAgent recommendationAgent; // Removed - agent architecture simplified

    // Popular stocks to recommend based on different risk profiles
    private static final String[] CONSERVATIVE_STOCKS = {"JNJ", "KO", "PG", "WMT", "MCD", "PEP"};
    private static final String[] MODERATE_STOCKS = {"AAPL", "MSFT", "GOOGL", "AMZN", "V", "MA", "NVDA", "TSLA"};
    private static final String[] AGGRESSIVE_STOCKS = {"TSLA", "NVDA", "AMD", "NFLX", "META", "AMD", "PLTR"};

    public RecommendationGenerationService(
            UserProfileRepository userProfileRepository,
            RecommendationRepository recommendationRepository,
            UserProfileAgent userProfileAgent,
            MarketAnalysisAgent marketAnalysisAgent
            // Removed agents - agent architecture simplified:
            // RiskAssessmentAgent riskAssessmentAgent,
            // ResearchAgent researchAgent,
            // RecommendationAgent recommendationAgent
    ) {
        this.userProfileRepository = userProfileRepository;
        this.recommendationRepository = recommendationRepository;
        this.userProfileAgent = userProfileAgent;
        this.marketAnalysisAgent = marketAnalysisAgent;
        // Removed agent assignments - agent architecture simplified
    }

    /**
     * Generate recommendations for a user based on their profile and portfolio
     * This is called asynchronously to avoid blocking
     */
    @Async
    public CompletableFuture<Void> generateRecommendationsForUser(String userId) {
        log.info("Generating recommendations for user: {}", userId);
        
        try {
            // Get user profile
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                log.info("No profile found for user {}, skipping recommendation generation", userId);
                return CompletableFuture.completedFuture(null);
            }

            UserProfile profile = profileOpt.get();
            String profileJson = userProfileAgent.getUserProfile(userId);

            // Get portfolio to see what they already own
            String portfolioSummary = userProfileAgent.getPortfolioSummary(userId);
            List<String> ownedSymbols = List.of();
            if (portfolioSummary.contains("\"exists\": true") && portfolioSummary.contains("\"symbols\"")) {
                // Extract symbols from portfolio summary
                String symbolsPart = portfolioSummary.substring(portfolioSummary.indexOf("\"symbols\": \"") + 12);
                symbolsPart = symbolsPart.substring(0, symbolsPart.indexOf("\""));
                if (!symbolsPart.equals("none")) {
                    ownedSymbols = List.of(symbolsPart.split(", "));
                }
            }

            // Select stocks to analyze based on risk tolerance
            String[] stocksToAnalyze = getStocksForRiskTolerance(profile.getRiskTolerance());
            
            // Limit to 3-5 recommendations
            int maxRecommendations = 5;
            int generated = 0;

            for (String symbol : stocksToAnalyze) {
                if (generated >= maxRecommendations) break;
                
                // Skip if user already owns this stock
                if (ownedSymbols.contains(symbol)) {
                    continue;
                }

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
                    // Generate recommendation using agents
                    String marketAnalysis = marketAnalysisAgent.analyzeTrends(symbol, "monthly");
                    // Removed agents - agent architecture simplified
                    // String riskAssessment = riskAssessmentAgent.assessStockRisk(symbol, "");
                    // String researchSummary = researchAgent.getCompanyFundamentals(symbol);

                    // Generate recommendation - using only market analysis now
                    // String recommendationResult = recommendationAgent.generateRecommendation(
                    //     userId, symbol, marketAnalysis, riskAssessment, researchSummary, profileJson
                    // );
                    String recommendationResult = "{\"error\": \"Recommendation generation disabled - agent architecture simplified\"}";

                    if (!recommendationResult.contains("\"error\"")) {
                        generated++;
                        log.info("Generated recommendation for {}: {}", symbol, recommendationResult);
                    } else {
                        log.warn("Failed to generate recommendation for {}: {}", symbol, recommendationResult);
                    }

                    // Add small delay to avoid rate limiting
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.warn("Error generating recommendation for {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Generated {} recommendations for user {}", generated, userId);
        } catch (Exception e) {
            log.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get stocks to analyze based on risk tolerance
     */
    private String[] getStocksForRiskTolerance(UserProfile.RiskTolerance riskTolerance) {
        return switch (riskTolerance) {
            case CONSERVATIVE -> CONSERVATIVE_STOCKS;
            case MODERATE -> MODERATE_STOCKS;
            case AGGRESSIVE -> AGGRESSIVE_STOCKS;
            default -> MODERATE_STOCKS;
        };
    }
}

