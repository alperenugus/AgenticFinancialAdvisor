package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.StockHolding;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.agent.financialadvisor.service.MarketDataService;
import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.aspect.ToolCallAspect;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserProfileAgent {

    private static final Logger log = LoggerFactory.getLogger(UserProfileAgent.class);
    private final UserProfileRepository userProfileRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataService marketDataService;
    private final WebSocketService webSocketService;

    @Autowired
    public UserProfileAgent(
            UserProfileRepository userProfileRepository,
            PortfolioRepository portfolioRepository,
            MarketDataService marketDataService,
            WebSocketService webSocketService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
        this.webSocketService = webSocketService;
    }

    @Tool("Get user's investment profile including risk tolerance, investment horizon, goals, and preferences. " +
          "Use this to understand the user's investment preferences before making recommendations. " +
          "Requires: userId (string). Returns user profile information.")
    @Transactional(readOnly = true)
    public String getUserProfile(String userId) {
        log.info("🔵 getUserProfile CALLED with userId={}", userId);
        
        // Send tool call notification
        String sessionId = ToolCallAspect.getSessionId();
        if (sessionId != null) {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("userId", userId.length() > 30 ? userId.substring(0, 27) + "..." : userId);
            webSocketService.sendToolCall(sessionId, "Get User Profile", params);
            webSocketService.sendReasoning(sessionId, "🔧 Retrieving user profile...");
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return String.format(
                    "{\"userId\": \"%s\", \"exists\": false, \"message\": \"User profile not found. User needs to create a profile first.\"}",
                    userId
                );
            }
            
            UserProfile profile = profileOpt.get();
            
            // Access all lazy collections while still in transaction
            List<String> goals = profile.getGoals() != null ? new ArrayList<>(profile.getGoals()) : new ArrayList<>();
            List<String> preferredSectors = profile.getPreferredSectors() != null ? new ArrayList<>(profile.getPreferredSectors()) : new ArrayList<>();
            List<String> excludedSectors = profile.getExcludedSectors() != null ? new ArrayList<>(profile.getExcludedSectors()) : new ArrayList<>();
            
            String result = String.format(
                "{\"userId\": \"%s\", \"exists\": true, \"riskTolerance\": \"%s\", \"horizon\": \"%s\", " +
                "\"goals\": %s, \"budget\": %s, \"preferredSectors\": %s, \"excludedSectors\": %s, " +
                "\"ethicalInvesting\": %s, \"message\": \"User profile retrieved\"}",
                profile.getUserId(),
                profile.getRiskTolerance(),
                profile.getHorizon(),
                goals.toString(),
                profile.getBudget() != null ? profile.getBudget().toString() : "null",
                preferredSectors.toString(),
                excludedSectors.toString(),
                profile.getEthicalInvesting()
            );
            
            // Send tool result notification
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                webSocketService.sendToolResult(sessionId, "Get User Profile", 
                    "Profile retrieved: " + profile.getRiskTolerance() + " risk, " + goals.size() + " goals", duration);
                webSocketService.sendReasoning(sessionId, "✅ User profile retrieved");
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error getting user profile: {}", e.getMessage(), e);
            
            // Send error notification
            if (sessionId != null) {
                webSocketService.sendReasoning(sessionId, "❌ Failed to get user profile: " + e.getMessage());
            }
            
            return String.format("{\"error\": \"Error getting user profile: %s\"}", e.getMessage());
        }
    }

    @Tool("Update user's risk tolerance level. Use this when the user wants to change their risk tolerance. " +
          "Requires: userId (string), riskTolerance (CONSERVATIVE, MODERATE, or AGGRESSIVE). " +
          "Returns updated profile.")
    public String updateRiskTolerance(String userId, String riskTolerance) {
        log.info("🔵 updateRiskTolerance CALLED with userId={}, riskTolerance={}", userId, riskTolerance);
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
            
            UserProfile.RiskTolerance tolerance;
            try {
                tolerance = UserProfile.RiskTolerance.valueOf(riskTolerance.toUpperCase());
            } catch (IllegalArgumentException e) {
                return String.format("{\"error\": \"Invalid risk tolerance: %s. Must be CONSERVATIVE, MODERATE, or AGGRESSIVE\"}", riskTolerance);
            }
            
            profile.setRiskTolerance(tolerance);
            userProfileRepository.save(profile);
            
            return String.format(
                "{\"userId\": \"%s\", \"riskTolerance\": \"%s\", \"message\": \"Risk tolerance updated successfully\"}",
                userId, tolerance
            );
        } catch (Exception e) {
            log.error("Error updating risk tolerance: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error updating risk tolerance: %s\"}", e.getMessage());
        }
    }

    @Tool("Get user's investment goals. Returns list of goals like RETIREMENT, GROWTH, INCOME. " +
          "Requires: userId (string).")
    @Transactional(readOnly = true)
    public String getInvestmentGoals(String userId) {
        log.info("🔵 getInvestmentGoals CALLED with userId={}", userId);
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return String.format("{\"userId\": \"%s\", \"goals\": [], \"message\": \"User profile not found\"}", userId);
            }
            
            // Access lazy collection while still in transaction
            List<String> goals = profileOpt.get().getGoals() != null ? new ArrayList<>(profileOpt.get().getGoals()) : new ArrayList<>();
            return String.format(
                "{\"userId\": \"%s\", \"goals\": %s, \"message\": \"Investment goals retrieved\"}",
                userId, goals.toString()
            );
        } catch (Exception e) {
            log.error("Error getting investment goals: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting investment goals: %s\"}", e.getMessage());
        }
    }

    @Tool("Get user's complete portfolio including all holdings, total value, and gain/loss. " +
          "Use this to understand what stocks the user currently owns before making recommendations. " +
          "Requires: userId (string). Returns portfolio with all holdings and summary.")
    @Transactional(readOnly = true)
    public String getPortfolio(String userId) {
        log.info("🔵 getPortfolio CALLED with userId={}", userId);
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                return String.format(
                    "{\"userId\": \"%s\", \"exists\": false, \"message\": \"Portfolio not found. User has no holdings yet.\"}",
                    userId
                );
            }

            Portfolio portfolio = portfolioOpt.get();
            
            // Refresh current prices for all holdings
            if (portfolio.getHoldings() != null && !portfolio.getHoldings().isEmpty()) {
                for (StockHolding holding : portfolio.getHoldings()) {
                    try {
                        BigDecimal currentPrice = marketDataService.getStockPrice(holding.getSymbol());
                        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                            holding.setCurrentPrice(currentPrice);
                            // @PreUpdate will handle value, gainLoss, gainLossPercent calculations
                        }
                    } catch (Exception e) {
                        log.warn("Could not refresh price for {}: {}", holding.getSymbol(), e.getMessage());
                    }
                }
                // Save to trigger @PreUpdate calculations
                portfolio = portfolioRepository.save(portfolio);
            }

            // Build holdings JSON
            String holdingsJson = portfolio.getHoldings().stream()
                .map(h -> String.format(
                    "{\"symbol\": \"%s\", \"quantity\": %d, \"averagePrice\": %s, \"currentPrice\": %s, " +
                    "\"value\": %s, \"gainLoss\": %s, \"gainLossPercent\": %s}",
                    h.getSymbol(),
                    h.getQuantity(),
                    h.getAveragePrice() != null ? h.getAveragePrice().toString() : "null",
                    h.getCurrentPrice() != null ? h.getCurrentPrice().toString() : "null",
                    h.getValue() != null ? h.getValue().toString() : "null",
                    h.getGainLoss() != null ? h.getGainLoss().toString() : "null",
                    h.getGainLossPercent() != null ? h.getGainLossPercent().toString() : "null"
                ))
                .collect(Collectors.joining(", ", "[", "]"));

            String result = String.format(
                "{\"userId\": \"%s\", \"exists\": true, \"totalValue\": %s, \"totalGainLoss\": %s, " +
                "\"totalGainLossPercent\": %s, \"holdings\": %s, \"holdingsCount\": %d, " +
                "\"message\": \"Portfolio retrieved with current prices\"}",
                userId,
                portfolio.getTotalValue() != null ? portfolio.getTotalValue().toString() : "0",
                portfolio.getTotalGainLoss() != null ? portfolio.getTotalGainLoss().toString() : "0",
                portfolio.getTotalGainLossPercent() != null ? portfolio.getTotalGainLossPercent().toString() : "0",
                holdingsJson,
                portfolio.getHoldings() != null ? portfolio.getHoldings().size() : 0
            );
            
            // Send tool result notification
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                int holdingsCount = portfolio.getHoldings() != null ? portfolio.getHoldings().size() : 0;
                webSocketService.sendToolResult(sessionId, "Get Portfolio", 
                    holdingsCount + " holdings, Total: $" + (portfolio.getTotalValue() != null ? portfolio.getTotalValue().toString() : "0"), duration);
                webSocketService.sendReasoning(sessionId, "✅ Portfolio retrieved: " + holdingsCount + " holdings");
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error getting portfolio: {}", e.getMessage(), e);
            
            // Send error notification
            if (sessionId != null) {
                webSocketService.sendReasoning(sessionId, "❌ Failed to get portfolio: " + e.getMessage());
            }
            
            return String.format("{\"error\": \"Error getting portfolio: %s\"}", e.getMessage());
        }
    }

    @Tool("Get user's portfolio holdings list. Returns just the list of stocks the user owns. " +
          "Use this when you need to know what stocks are in the portfolio. " +
          "Requires: userId (string). Returns list of holdings with symbols and quantities.")
    @Transactional(readOnly = true)
    public String getPortfolioHoldings(String userId) {
        log.info("🔵 getPortfolioHoldings CALLED with userId={}", userId);
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty() || portfolioOpt.get().getHoldings() == null || 
                portfolioOpt.get().getHoldings().isEmpty()) {
                return String.format(
                    "{\"userId\": \"%s\", \"holdings\": [], \"message\": \"Portfolio is empty or not found\"}",
                    userId
                );
            }

            Portfolio portfolio = portfolioOpt.get();
            String holdingsList = portfolio.getHoldings().stream()
                .map(h -> String.format("\"%s\" (%d shares)", h.getSymbol(), h.getQuantity()))
                .collect(Collectors.joining(", "));

            String holdingsJson = portfolio.getHoldings().stream()
                .map(h -> String.format(
                    "{\"symbol\": \"%s\", \"quantity\": %d, \"averagePrice\": %s}",
                    h.getSymbol(),
                    h.getQuantity(),
                    h.getAveragePrice() != null ? h.getAveragePrice().toString() : "null"
                ))
                .collect(Collectors.joining(", ", "[", "]"));

            return String.format(
                "{\"userId\": \"%s\", \"holdings\": %s, \"holdingsList\": \"%s\", \"count\": %d, " +
                "\"message\": \"Portfolio holdings retrieved\"}",
                userId,
                holdingsJson,
                holdingsList,
                portfolio.getHoldings().size()
            );
        } catch (Exception e) {
            log.error("Error getting portfolio holdings: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting portfolio holdings: %s\"}", e.getMessage());
        }
    }

    @Tool("Get user's portfolio summary (total value, gain/loss, number of holdings). " +
          "Use this for quick portfolio overview without full details. " +
          "Requires: userId (string). Returns portfolio summary statistics.")
    @Transactional(readOnly = true)
    public String getPortfolioSummary(String userId) {
        log.info("🔵 getPortfolioSummary CALLED with userId={}", userId);
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                return String.format(
                    "{\"userId\": \"%s\", \"exists\": false, \"message\": \"Portfolio not found\"}",
                    userId
                );
            }

            Portfolio portfolio = portfolioOpt.get();
            
            // Refresh prices if holdings exist
            if (portfolio.getHoldings() != null && !portfolio.getHoldings().isEmpty()) {
                for (StockHolding holding : portfolio.getHoldings()) {
                    try {
                        BigDecimal currentPrice = marketDataService.getStockPrice(holding.getSymbol());
                        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                            holding.setCurrentPrice(currentPrice);
                            // @PreUpdate will handle value, gainLoss, gainLossPercent calculations
                        }
                    } catch (Exception e) {
                        log.warn("Could not refresh price for {}: {}", holding.getSymbol(), e.getMessage());
                    }
                }
                // Save to trigger @PreUpdate calculations
                portfolio = portfolioRepository.save(portfolio);
            }

            int holdingsCount = portfolio.getHoldings() != null ? portfolio.getHoldings().size() : 0;
            String symbols = portfolio.getHoldings() != null && !portfolio.getHoldings().isEmpty()
                ? portfolio.getHoldings().stream()
                    .map(StockHolding::getSymbol)
                    .collect(Collectors.joining(", "))
                : "none";

            return String.format(
                "{\"userId\": \"%s\", \"exists\": true, \"totalValue\": %s, \"totalGainLoss\": %s, " +
                "\"totalGainLossPercent\": %s, \"holdingsCount\": %d, \"symbols\": \"%s\", " +
                "\"message\": \"Portfolio summary retrieved\"}",
                userId,
                portfolio.getTotalValue() != null ? portfolio.getTotalValue().toString() : "0",
                portfolio.getTotalGainLoss() != null ? portfolio.getTotalGainLoss().toString() : "0",
                portfolio.getTotalGainLossPercent() != null ? portfolio.getTotalGainLossPercent().toString() : "0",
                holdingsCount,
                symbols
            );
        } catch (Exception e) {
            log.error("Error getting portfolio summary: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting portfolio summary: %s\"}", e.getMessage());
        }
    }
}


