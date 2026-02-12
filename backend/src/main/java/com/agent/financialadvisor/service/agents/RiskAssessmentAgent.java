package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.agent.financialadvisor.service.MarketDataService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Service
public class RiskAssessmentAgent {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentAgent.class);
    private final MarketDataService marketDataService;
    private final PortfolioRepository portfolioRepository;
    private final UserProfileRepository userProfileRepository;

    public RiskAssessmentAgent(
            MarketDataService marketDataService,
            PortfolioRepository portfolioRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.marketDataService = marketDataService;
        this.portfolioRepository = portfolioRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Tool("Assess the risk level of an individual stock. Use this to evaluate how risky a stock investment is. " +
          "Requires: symbol (string), metrics (optional JSON string with additional metrics). " +
          "Returns risk level (LOW, MEDIUM, HIGH) with reasoning.")
    public String assessStockRisk(String symbol, String metrics) {
        log.info("🔵 assessStockRisk CALLED with symbol={}, metrics={}", symbol, metrics);
        try {
            // Get current price and price data
            BigDecimal currentPrice = marketDataService.getStockPrice(symbol);
            Map<String, Object> priceData = marketDataService.getStockPriceData(symbol, "daily");
            
            if (currentPrice == null || priceData.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to assess risk - insufficient data.\"}", symbol);
            }

            BigDecimal high = (BigDecimal) priceData.get("high");
            BigDecimal low = (BigDecimal) priceData.get("low");
            BigDecimal average = (BigDecimal) priceData.get("average");

            if (high == null || low == null || average == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Insufficient data for risk assessment.\"}", symbol);
            }

            // Calculate volatility (price range as percentage of average)
            BigDecimal range = high.subtract(low);
            BigDecimal volatility = range.divide(average, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Determine risk level based on volatility
            String riskLevel;
            String reasoning;
            
            if (volatility.compareTo(BigDecimal.valueOf(10)) < 0) {
                riskLevel = "LOW";
                reasoning = String.format("Low volatility (%.2f%%). Stock price is relatively stable.", volatility.doubleValue());
            } else if (volatility.compareTo(BigDecimal.valueOf(25)) < 0) {
                riskLevel = "MEDIUM";
                reasoning = String.format("Moderate volatility (%.2f%%). Stock shows normal price fluctuations.", volatility.doubleValue());
            } else {
                riskLevel = "HIGH";
                reasoning = String.format("High volatility (%.2f%%). Stock price is highly volatile and unpredictable.", volatility.doubleValue());
            }

            // Additional risk factors
            BigDecimal priceDeviation = currentPrice.subtract(average).abs()
                    .divide(average, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            if (priceDeviation.compareTo(BigDecimal.valueOf(20)) > 0) {
                riskLevel = "HIGH";
                reasoning += String.format(" Current price deviates significantly (%.2f%%) from average.", priceDeviation.doubleValue());
            }

            return String.format(
                "{\"symbol\": \"%s\", \"riskLevel\": \"%s\", \"volatility\": \"%.2f%%\", " +
                "\"currentPrice\": %s, \"averagePrice\": %s, \"reasoning\": \"%s\"}",
                symbol, riskLevel, volatility.doubleValue(), currentPrice, average, reasoning
            );
        } catch (Exception e) {
            log.error("Error assessing stock risk for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error assessing risk: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Calculate overall portfolio risk. Use this to assess the total risk of a user's portfolio. " +
          "Requires: userId (string). Returns portfolio risk assessment with diversification analysis.")
    public String calculatePortfolioRisk(String userId) {
        log.info("🔵 calculatePortfolioRisk CALLED with userId={}", userId);
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                return String.format("{\"userId\": \"%s\", \"error\": \"Portfolio not found for user.\"}", userId);
            }

            Portfolio portfolio = portfolioOpt.get();
            if (portfolio.getHoldings() == null || portfolio.getHoldings().isEmpty()) {
                return String.format("{\"userId\": \"%s\", \"riskLevel\": \"LOW\", \"message\": \"Portfolio is empty - no risk.\"}", userId);
            }

            // Calculate weighted average risk
            int totalHoldings = portfolio.getHoldings().size();
            int highRiskCount = 0;
            int mediumRiskCount = 0;
            int lowRiskCount = 0;

            for (var holding : portfolio.getHoldings()) {
                String riskAssessment = assessStockRisk(holding.getSymbol(), "");
                // Parse risk level from JSON (simplified)
                if (riskAssessment.contains("\"riskLevel\": \"HIGH\"")) {
                    highRiskCount++;
                } else if (riskAssessment.contains("\"riskLevel\": \"MEDIUM\"")) {
                    mediumRiskCount++;
                } else {
                    lowRiskCount++;
                }
            }

            // Determine overall portfolio risk
            String portfolioRiskLevel;
            String diversification;
            
            if (highRiskCount > totalHoldings / 2) {
                portfolioRiskLevel = "HIGH";
            } else if (mediumRiskCount > totalHoldings / 2 || highRiskCount > 0) {
                portfolioRiskLevel = "MEDIUM";
            } else {
                portfolioRiskLevel = "LOW";
            }

            // Diversification assessment
            if (totalHoldings < 3) {
                diversification = "LOW - Portfolio is not well diversified. Consider adding more holdings.";
            } else if (totalHoldings < 10) {
                diversification = "MODERATE - Portfolio has some diversification.";
            } else {
                diversification = "GOOD - Portfolio is well diversified.";
            }

            return String.format(
                "{\"userId\": \"%s\", \"portfolioRiskLevel\": \"%s\", \"totalHoldings\": %d, " +
                "\"highRiskHoldings\": %d, \"mediumRiskHoldings\": %d, \"lowRiskHoldings\": %d, " +
                "\"diversification\": \"%s\", \"totalValue\": %s}",
                userId, portfolioRiskLevel, totalHoldings, highRiskCount, mediumRiskCount, lowRiskCount,
                diversification, portfolio.getTotalValue() != null ? portfolio.getTotalValue() : "0"
            );
        } catch (Exception e) {
            log.error("Error calculating portfolio risk for {}: {}", userId, e.getMessage(), e);
            return String.format("{\"userId\": \"%s\", \"error\": \"Error calculating portfolio risk: %s\"}", userId, e.getMessage());
        }
    }

    @Tool("Check if a stock's risk level is compatible with user's risk tolerance. " +
          "Use this to ensure recommendations match user preferences. " +
          "Requires: userId (string), symbol (string). " +
          "Returns compatibility assessment and warnings if risk mismatch.")
    public String checkRiskTolerance(String userId, String symbol) {
        log.info("🔵 checkRiskTolerance CALLED with userId={}, symbol={}", userId, symbol);
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return String.format("{\"userId\": \"%s\", \"symbol\": \"%s\", \"error\": \"User profile not found.\"}", userId, symbol);
            }

            UserProfile profile = profileOpt.get();
            String stockRisk = assessStockRisk(symbol, "");
            
            // Parse risk level from stock assessment
            String stockRiskLevel = "MEDIUM"; // default
            if (stockRisk.contains("\"riskLevel\": \"HIGH\"")) {
                stockRiskLevel = "HIGH";
            } else if (stockRisk.contains("\"riskLevel\": \"LOW\"")) {
                stockRiskLevel = "LOW";
            }

            UserProfile.RiskTolerance userTolerance = profile.getRiskTolerance();
            boolean compatible = false;
            String warning = "";

            switch (userTolerance) {
                case CONSERVATIVE:
                    compatible = stockRiskLevel.equals("LOW");
                    if (!compatible) {
                        warning = "WARNING: This stock is " + stockRiskLevel + " risk, but user prefers CONSERVATIVE (LOW risk) investments.";
                    }
                    break;
                case MODERATE:
                    compatible = !stockRiskLevel.equals("HIGH");
                    if (!compatible) {
                        warning = "WARNING: This stock is HIGH risk, but user prefers MODERATE risk investments.";
                    }
                    break;
                case AGGRESSIVE:
                    compatible = true; // Aggressive investors can handle any risk
                    break;
            }

            return String.format(
                "{\"userId\": \"%s\", \"symbol\": \"%s\", \"userTolerance\": \"%s\", " +
                "\"stockRiskLevel\": \"%s\", \"compatible\": %s, \"warning\": \"%s\"}",
                userId, symbol, userTolerance, stockRiskLevel, compatible, warning
            );
        } catch (Exception e) {
            log.error("Error checking risk tolerance for {}: {}", userId, e.getMessage(), e);
            return String.format("{\"userId\": \"%s\", \"symbol\": \"%s\", \"error\": \"Error checking risk tolerance: %s\"}", userId, symbol, e.getMessage());
        }
    }

    @Tool("Get risk metrics for a stock including volatility and beta (simplified). " +
          "Requires: symbol (string). Returns risk metrics like volatility percentage.")
    public String getRiskMetrics(String symbol) {
        log.info("🔵 getRiskMetrics CALLED with symbol={}", symbol);
        try {
            Map<String, Object> priceData = marketDataService.getStockPriceData(symbol, "daily");
            BigDecimal currentPrice = marketDataService.getStockPrice(symbol);
            
            if (priceData.isEmpty() || currentPrice == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to calculate risk metrics.\"}", symbol);
            }

            BigDecimal high = (BigDecimal) priceData.get("high");
            BigDecimal low = (BigDecimal) priceData.get("low");
            BigDecimal average = (BigDecimal) priceData.get("average");

            if (high == null || low == null || average == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Insufficient data for risk metrics.\"}", symbol);
            }

            // Calculate volatility
            BigDecimal range = high.subtract(low);
            BigDecimal volatility = range.divide(average, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Simplified beta (assuming 1.0 for most stocks - would need market comparison)
            BigDecimal beta = BigDecimal.valueOf(1.0);

            return String.format(
                "{\"symbol\": \"%s\", \"volatility\": \"%.2f%%\", \"beta\": %s, " +
                "\"priceRange\": {\"high\": %s, \"low\": %s, \"average\": %s}, " +
                "\"currentPrice\": %s}",
                symbol, volatility.doubleValue(), beta, high, low, average, currentPrice
            );
        } catch (Exception e) {
            log.error("Error getting risk metrics for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error calculating risk metrics: %s\"}", symbol, e.getMessage());
        }
    }
}

