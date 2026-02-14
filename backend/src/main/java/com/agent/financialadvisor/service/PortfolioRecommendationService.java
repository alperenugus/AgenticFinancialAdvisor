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
                    .map(h -> h.getSymbol().toUpperCase().trim())
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
                log.info("📊 User {} has {} holdings: {}", userId, ownedSymbols.size(), ownedSymbols);
            } else {
                log.warn("⚠️ User {} has no portfolio or holdings", userId);
            }

            // PRIMARY: Generate recommendations for stocks the user already owns
            List<String> stocksToAnalyze = new ArrayList<>(ownedSymbols);
            
            // SECONDARY: Also discover new stocks if user has less than 5 holdings
            if (ownedSymbols.size() < 5) {
                String excludeOwned = String.join(",", ownedSymbols);
                String discoveryResult = stockDiscoveryAgent.discoverStocks(
                    profile.getRiskTolerance().toString(),
                    profile.getPreferredSectors() != null ? String.join(",", profile.getPreferredSectors()) : "",
                    excludeOwned
                );
                List<String> discoveredStocks = parseDiscoveredStocks(discoveryResult);
                // Add discovered stocks up to 5 total
                for (String symbol : discoveredStocks) {
                    if (stocksToAnalyze.size() >= 5) break;
                    if (!stocksToAnalyze.contains(symbol)) {
                        stocksToAnalyze.add(symbol);
                    }
                }
            }
            
            if (stocksToAnalyze.isEmpty()) {
                log.warn("No stocks to analyze for user {} (no holdings and no discoveries)", userId);
                return CompletableFuture.completedFuture(null);
            }

            log.info("🎯 Generating recommendations for {} stocks for user {}: {}", stocksToAnalyze.size(), userId, stocksToAnalyze);
            
            // Log current recommendations in DB before generation
            List<Recommendation> existingBefore = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.info("📋 Current recommendations in DB before generation: {} (symbols: {})", 
                existingBefore.size(), 
                existingBefore.stream().map(r -> r.getSymbol() + "(" + r.getAction() + ")").toList());

            int generated = 0;
            int failed = 0;
            String sessionId = "portfolio-recommendation-" + userId + "-" + System.currentTimeMillis();

            // Generate recommendations for each stock (prioritize owned stocks)
            for (String symbol : stocksToAnalyze) {
                try {
                    // Delete ALL existing recommendations for this stock to ensure only one per stock
                    // Use repository method for case-insensitive deletion
                    try {
                        recommendationRepository.deleteByUserIdAndSymbolIgnoreCase(userId, symbol);
                        log.info("🗑️ Deleted all existing recommendations for {} (user: {})", symbol, userId);
                        // Small delay to ensure DB commit
                        Thread.sleep(100);
                    } catch (Exception e) {
                        log.warn("Could not delete existing recommendations for {}: {}", symbol, e.getMessage());
                        // Try manual deletion as fallback
                        List<Recommendation> existingRecommendations = recommendationRepository
                            .findByUserIdOrderByCreatedAtDesc(userId)
                            .stream()
                            .filter(r -> r.getSymbol().equalsIgnoreCase(symbol))
                            .toList();
                        if (!existingRecommendations.isEmpty()) {
                            recommendationRepository.deleteAll(existingRecommendations);
                            log.info("🗑️ Manually deleted {} existing recommendation(s) for {} (user: {})", 
                                existingRecommendations.size(), symbol, userId);
                        }
                    }
                    
                    log.info("🔄 Generating recommendation {}/{} for {} (user: {})", 
                        generated + 1, stocksToAnalyze.size(), symbol, userId);

                    // Use orchestrator to analyze and recommend with comprehensive research
                    String query = String.format(
                        "As a SENIOR FINANCIAL ANALYST with expertise in technical analysis, fundamental analysis, market sentiment, and portfolio management, " +
                        "provide a COMPREHENSIVE, RESEARCH-DRIVEN portfolio recommendation for %s. " +
                        "User profile: risk tolerance=%s, investment horizon=%s, goals=%s. " +
                        "Current portfolio holdings: %s. " +
                        "CRITICAL: You MUST conduct THOROUGH RESEARCH using ALL available tools. This is a professional analysis that will guide investment decisions. " +
                        "MANDATORY RESEARCH STEPS (execute ALL of these): " +
                        "1. Call getStockPrice(%s) to get the CURRENT stock price - this is your baseline " +
                        "2. Call getStockPriceData(%s, 'daily') AND getStockPriceData(%s, 'weekly') to get comprehensive price history " +
                        "3. Call analyzeTrends(%s, 'daily') AND analyzeTrends(%s, 'weekly') to identify chart patterns across timeframes " +
                        "4. Call getCompanyOverview(%s) to get fundamental data (P/E, P/B, revenue growth, profit margin, etc.) " +
                        "5. Call assessRisk(%s) to get risk metrics (volatility, beta, etc.) " +
                        "6. Call getMarketNews(%s) to understand market sentiment and recent news " +
                        "7. Call getTechnicalIndicators(%s) to get technical analysis metrics " +
                        "8. Analyze the data comprehensively: " +
                        "   - Technical patterns: Look for head and shoulders, double tops/bottoms, triangles, support/resistance, trendlines " +
                        "   - Fundamental strength: Evaluate P/E ratio, revenue growth, profit margins, dividend yield " +
                        "   - Market sentiment: Consider recent news, analyst sentiment, market trends " +
                        "   - Risk assessment: Evaluate volatility, beta, price stability " +
                        "   - Portfolio fit: Consider how this stock fits with user's existing holdings and risk tolerance " +
                        "COMPREHENSIVE RECOMMENDATION MUST INCLUDE: " +
                        "1. Current stock price (from getStockPrice) " +
                        "2. Technical analysis: Specific chart patterns identified (e.g., 'I identify a bullish engulfing pattern on daily chart and rising trendline on weekly chart') " +
                        "3. Fundamental analysis: Key metrics and what they mean (e.g., 'P/E ratio of 35.1 indicates...') " +
                        "4. Market sentiment: Recent news and trends affecting the stock " +
                        "5. Risk assessment: Specific risk level and why " +
                        "6. Stop loss level: Calculate as percentage below current price (e.g., 'For this stock, you can have a stop loss at $X, which is Y%% below current price') " +
                        "7. Averaging down advice: If applicable, specific price level (e.g., 'For this stock, you can average down a bit if price reaches $X') " +
                        "8. Entry price: If buying, specific entry level " +
                        "9. Target price: Specific target with reasoning based on technical/fundamental analysis " +
                        "10. Exit price: Specific exit level for profit taking " +
                        "11. Portfolio fit: How this recommendation aligns with user's risk tolerance, goals, and existing holdings " +
                        "12. Clear BUY/SELL/HOLD recommendation with strong reasoning " +
                        "Format: 'For %s, the current price is $X (from getStockPrice). After comprehensive analysis: [detailed analysis]. " +
                        "I recommend [BUY/SELL/HOLD] because [strong reasoning]. For this stock, you can have a stop loss at $Y...' " +
                        "Be specific with ACTUAL prices, percentages, and data. NEVER use placeholders. Provide professional-grade analysis.",
                        symbol,
                        profile.getRiskTolerance(),
                        profile.getHorizon(),
                        profile.getGoals() != null ? profile.getGoals().toString() : "[]",
                        ownedSymbols.isEmpty() ? "no holdings" : String.join(", ", ownedSymbols),
                        symbol, symbol, symbol, symbol, symbol, symbol, symbol, symbol, symbol
                    );

                    // Use orchestrator to generate recommendation
                    String recommendationText = orchestratorService.coordinateAnalysis(userId, query, sessionId + "-" + symbol);
                    
                    // Parse recommendation from orchestrator response and save
                    saveRecommendationFromOrchestrator(userId, symbol, recommendationText, profile);

                    generated++;
                    log.info("✅ Successfully generated portfolio recommendation {}/{} for {} (user: {})", 
                        generated, stocksToAnalyze.size(), symbol, userId);
                    log.debug("Recommendation preview: {}", recommendationText.substring(0, Math.min(200, recommendationText.length())));

                    // Add delay to avoid rate limiting
                    Thread.sleep(2000);
                } catch (Exception e) {
                    failed++;
                    log.error("❌ Error generating portfolio recommendation for {} (user: {}): {}", 
                        symbol, userId, e.getMessage(), e);
                    log.error("Stack trace:", e);
                    // Continue with next stock instead of breaking
                }
            }
            
            log.info("📊 Generation summary: Generated: {}, Failed: {}, Total stocks: {} for user {}", 
                generated, failed, stocksToAnalyze.size(), userId);
            
            // Final verification: Check how many recommendations were actually saved
            List<Recommendation> savedRecommendations = recommendationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
            log.info("📊 Recommendation generation complete. Generated: {}, Saved in DB: {} for user {}", 
                generated, savedRecommendations.size(), userId);
            if (savedRecommendations.size() != stocksToAnalyze.size()) {
                log.warn("⚠️ Mismatch: Expected {} recommendations but found {} in database for user {}", 
                    stocksToAnalyze.size(), savedRecommendations.size(), userId);
                log.info("Stocks analyzed: {}", stocksToAnalyze);
                log.info("Recommendations in DB: {}", savedRecommendations.stream()
                    .map(r -> r.getSymbol() + "(" + r.getAction() + ")")
                    .toList());
            }

            log.info("Generated {} portfolio recommendations for user {} (analyzed {} stocks)", 
                generated, userId, stocksToAnalyze.size());
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
            log.info("💾 Saving recommendation for {} (user: {})", symbol, userId);
            
            // Double-check: Delete any remaining duplicates for this symbol
            List<Recommendation> duplicates = recommendationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(r -> r.getSymbol().equalsIgnoreCase(symbol))
                .toList();
            if (!duplicates.isEmpty()) {
                log.warn("Found {} duplicate recommendation(s) for {} (user: {}), deleting them", 
                    duplicates.size(), symbol, userId);
                recommendationRepository.deleteAll(duplicates);
            }
            
            Recommendation recommendation = new Recommendation();
            recommendation.setUserId(userId);
            recommendation.setSymbol(symbol.toUpperCase()); // Ensure uppercase
            
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

