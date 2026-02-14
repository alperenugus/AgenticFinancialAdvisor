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

import java.math.BigDecimal;
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

            log.info("Discovered {} stocks for user {}: {}", discoveredStocks.size(), userId, discoveredStocks);

            // Limit to 5 recommendations
            int maxRecommendations = 5;
            int generated = 0;
            int skipped = 0;
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
                        skipped++;
                        continue;
                    }
                }
                
                log.info("Generating recommendation {}/{} for {} (user: {})", generated + 1, maxRecommendations, symbol, userId);

                try {
                    // Use orchestrator to analyze and recommend with professional financial analyst approach
                    String query = String.format(
                        "As a professional financial analyst, provide a comprehensive portfolio recommendation for %s. " +
                        "User profile: risk tolerance=%s, investment horizon=%s, goals=%s. " +
                        "Current portfolio holdings: %s. " +
                        "CRITICAL: You MUST use the available tools to get REAL, CURRENT data. Do NOT use placeholders like [$Current Price] or [Stop Loss Price]. " +
                        "REQUIRED STEPS: " +
                        "1. First, call getStockPrice(%s) to get the CURRENT stock price - use this actual price in your recommendation " +
                        "2. Call getStockPriceData(%s, 'daily') to get price history for technical analysis " +
                        "3. Call analyzeTrends(%s, 'daily') to identify chart patterns " +
                        "4. Call getCompanyOverview(%s) to get fundamental data " +
                        "5. Call assessRisk(%s) to get risk metrics " +
                        "6. Use the ACTUAL current price to calculate stop loss (e.g., 5%% below current price) " +
                        "7. Use the ACTUAL current price to suggest entry/exit levels " +
                        "Provide a professional financial analyst-level recommendation including: " +
                        "1. Technical analysis patterns (head and shoulders, support/resistance, chart patterns) - MUST use getStockPriceData and analyzeTrends tools " +
                        "2. Stop loss level (specific price based on current price, e.g., 'For this stock, you can have a stop loss at $X' where X is calculated from current price) " +
                        "3. Averaging down advice if applicable (e.g., 'For this stock, you can average down a bit if price reaches $X' where X is calculated) " +
                        "4. Entry price suggestion (based on current price) " +
                        "5. Target price with reasoning (based on technical analysis) " +
                        "6. Exit price for profit taking (based on current price and target) " +
                        "7. Portfolio diversification analysis considering current holdings " +
                        "8. Risk assessment aligned with user's risk tolerance " +
                        "Format like: 'For %s, the current price is $X. I identify [PATTERN] on [TIMEFRAME]... For this stock, you can have a stop loss at $Y (Y%% below current price)...' " +
                        "Be specific with ACTUAL price levels, percentages, and technical analysis. NEVER use placeholders.",
                        symbol,
                        profile.getRiskTolerance(),
                        profile.getHorizon(),
                        profile.getGoals() != null ? profile.getGoals().toString() : "[]",
                        ownedSymbols.isEmpty() ? "no holdings" : String.join(", ", ownedSymbols),
                        symbol, symbol, symbol, symbol, symbol, symbol
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

            log.info("Generated {} portfolio recommendations for user {} (skipped {} recent ones, discovered {} total stocks)", 
                generated, userId, skipped, discoveredStocks.size());
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
            log.info("Parsing discovery result: {}", discoveryResult);
            JsonNode json = objectMapper.readTree(discoveryResult);
            if (json.has("discoveredStocks") && json.get("discoveredStocks").isArray()) {
                for (JsonNode stock : json.get("discoveredStocks")) {
                    String symbol = stock.asText();
                    if (symbol != null && !symbol.trim().isEmpty()) {
                        stocks.add(symbol.trim().toUpperCase());
                    }
                }
                log.info("Parsed {} stocks from discovery result: {}", stocks.size(), stocks);
            } else {
                log.warn("No 'discoveredStocks' array found in discovery result: {}", discoveryResult);
            }
        } catch (Exception e) {
            log.error("Error parsing discovered stocks from: {}", discoveryResult, e);
        }
        return stocks;
    }

    /**
     * Save recommendation from orchestrator response with professional financial analyst details
     */
    private void saveRecommendationFromOrchestrator(String userId, String symbol, String recommendationText, UserProfile profile) {
        try {
            Recommendation recommendation = new Recommendation();
            recommendation.setUserId(userId);
            recommendation.setSymbol(symbol);
            
            // Parse action from text
            String upperText = recommendationText.toUpperCase();
            if (upperText.contains("BUY") || upperText.contains("PURCHASE") || upperText.contains("ACQUIRE")) {
                recommendation.setAction(Recommendation.RecommendationAction.BUY);
            } else if (upperText.contains("SELL") || upperText.contains("EXIT") || upperText.contains("LIQUIDATE")) {
                recommendation.setAction(Recommendation.RecommendationAction.SELL);
            } else {
                recommendation.setAction(Recommendation.RecommendationAction.HOLD);
            }
            
            // Extract stop loss price
            BigDecimal stopLoss = extractPrice(recommendationText, "stop loss", "stop-loss", "stoploss");
            if (stopLoss != null) {
                recommendation.setStopLossPrice(stopLoss);
            }
            
            // Extract entry price
            BigDecimal entryPrice = extractPrice(recommendationText, "entry", "enter at", "entry price", "buy at");
            if (entryPrice != null) {
                recommendation.setEntryPrice(entryPrice);
            }
            
            // Extract exit price
            BigDecimal exitPrice = extractPrice(recommendationText, "exit", "exit at", "exit price", "take profit", "profit target");
            if (exitPrice != null) {
                recommendation.setExitPrice(exitPrice);
            }
            
            // Extract target price (if not already set as exit price)
            BigDecimal targetPrice = extractPrice(recommendationText, "target", "target price", "price target");
            if (targetPrice != null && recommendation.getExitPrice() == null) {
                recommendation.setExitPrice(targetPrice);
            }
            if (targetPrice != null) {
                recommendation.setTargetPrice(targetPrice);
            }
            
            // Extract technical patterns
            String technicalPatterns = extractTechnicalPatterns(recommendationText);
            if (technicalPatterns != null && !technicalPatterns.isEmpty()) {
                recommendation.setTechnicalPatterns(technicalPatterns);
            }
            
            // Extract averaging down advice
            String averagingDown = extractAveragingDownAdvice(recommendationText);
            if (averagingDown != null && !averagingDown.isEmpty()) {
                recommendation.setAveragingDownAdvice(averagingDown);
            }
            
            // Store full professional analysis
            recommendation.setProfessionalAnalysis(recommendationText);
            recommendation.setReasoning(recommendationText); // Also store in reasoning for backward compatibility
            
            // Set defaults based on profile
            recommendation.setConfidence(0.75); // Higher confidence for professional analysis
            recommendation.setRiskLevel(profile.getRiskTolerance() == UserProfile.RiskTolerance.CONSERVATIVE 
                ? Recommendation.RiskLevel.LOW 
                : profile.getRiskTolerance() == UserProfile.RiskTolerance.AGGRESSIVE 
                    ? Recommendation.RiskLevel.HIGH 
                    : Recommendation.RiskLevel.MEDIUM);
            recommendation.setTimeHorizon(profile.getHorizon() == UserProfile.InvestmentHorizon.SHORT
                ? Recommendation.InvestmentHorizon.SHORT
                : profile.getHorizon() == UserProfile.InvestmentHorizon.LONG
                    ? Recommendation.InvestmentHorizon.LONG
                    : Recommendation.InvestmentHorizon.MEDIUM);
            
            recommendationRepository.save(recommendation);
            log.info("Saved professional recommendation for {} with stop loss: {}, entry: {}, exit: {}", 
                symbol, stopLoss, entryPrice, exitPrice);
        } catch (Exception e) {
            log.error("Error saving recommendation for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Extract price from text using various patterns
     */
    private BigDecimal extractPrice(String text, String... keywords) {
        try {
            // Pattern to match prices: $123.45, $123, 123.45, etc.
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile(
                "\\$?([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{1,2})?)"
            );
            
            String lowerText = text.toLowerCase();
            for (String keyword : keywords) {
                int keywordIndex = lowerText.indexOf(keyword.toLowerCase());
                if (keywordIndex >= 0) {
                    // Look for price within 100 characters after keyword
                    String searchArea = text.substring(keywordIndex, Math.min(keywordIndex + 100, text.length()));
                    java.util.regex.Matcher matcher = pricePattern.matcher(searchArea);
                    if (matcher.find()) {
                        String priceStr = matcher.group(1).replace(",", "");
                        return new BigDecimal(priceStr);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting price: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract technical patterns from text
     */
    private String extractTechnicalPatterns(String text) {
        StringBuilder patterns = new StringBuilder();
        String lowerText = text.toLowerCase();
        
        // Common technical patterns
        String[] patternKeywords = {
            "head and shoulders", "double top", "double bottom", "triple top", "triple bottom",
            "ascending triangle", "descending triangle", "symmetrical triangle",
            "cup and handle", "flag", "pennant", "wedge",
            "support", "resistance", "breakout", "breakdown",
            "moving average", "rsi", "macd", "bollinger bands",
            "fibonacci", "elliot wave", "candlestick pattern"
        };
        
        for (String pattern : patternKeywords) {
            if (lowerText.contains(pattern)) {
                if (patterns.length() > 0) {
                    patterns.append(", ");
                }
                // Extract sentence containing the pattern
                int index = lowerText.indexOf(pattern);
                int start = Math.max(0, index - 50);
                int end = Math.min(text.length(), index + pattern.length() + 100);
                String context = text.substring(start, end).trim();
                patterns.append(context);
            }
        }
        
        return patterns.length() > 0 ? patterns.toString() : null;
    }

    /**
     * Extract averaging down advice from text
     */
    private String extractAveragingDownAdvice(String text) {
        String lowerText = text.toLowerCase();
        if (lowerText.contains("average down") || lowerText.contains("averaging down") || 
            lowerText.contains("dollar cost average") || lowerText.contains("add to position")) {
            // Extract the sentence or paragraph containing averaging down advice
            int index = lowerText.indexOf("average");
            if (index < 0) index = lowerText.indexOf("add to position");
            if (index >= 0) {
                int start = Math.max(0, index - 30);
                int end = Math.min(text.length(), index + 200);
                return text.substring(start, end).trim();
            }
        }
        return null;
    }
}

