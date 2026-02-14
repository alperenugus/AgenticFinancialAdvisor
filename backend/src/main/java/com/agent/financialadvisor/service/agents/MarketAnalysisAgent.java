package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.aspect.ToolCallAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class MarketAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(MarketAnalysisAgent.class);
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    @Autowired
    public MarketAnalysisAgent(
            MarketDataService marketDataService, 
            ObjectMapper objectMapper,
            WebSocketService webSocketService
    ) {
        this.marketDataService = marketDataService;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
    }

    @Tool("Get current stock price for a symbol. **MANDATORY**: You MUST call this tool whenever the user asks about a stock price, asks 'what is the price of X', asks 'X stock price', or mentions any stock symbol. " +
          "This tool fetches the LATEST AVAILABLE stock price from the API (no caching, always fresh). " +
          "**CRITICAL**: Do NOT use training data prices - they are outdated. You MUST call this tool to get current prices. " +
          "Note: Free tier may have 15-minute delay during market hours. Premium tier provides real-time data. " +
          "Requires: symbol (string, e.g., 'AAPL' for Apple, 'MSFT' for Microsoft). Returns current price as a number with timestamp. " +
          "**When to use**: ANY time you need to mention a stock price, even if you think you know it from training data.")
    public String getStockPrice(String symbol) {
        log.info("🔵 getStockPrice CALLED with symbol={} - FETCHING FRESH DATA FROM API", symbol);
        
        // Tool call tracking (for logging only)
        String sessionId = ToolCallAspect.getSessionId();
        
        long startTime = System.currentTimeMillis();
        try {
            BigDecimal price = marketDataService.getStockPrice(symbol);
            if (price == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to fetch stock price. Symbol may be invalid or API limit reached.\"}", symbol);
            }
            // Include timestamp to show data freshness
            String timestamp = java.time.LocalDateTime.now().toString();
            String result = String.format(
                "{\"symbol\": \"%s\", \"price\": %s, \"currency\": \"USD\", \"fetchedAt\": \"%s\", " +
                "\"note\": \"Fresh data fetched from API. Free tier may have 15-minute delay during market hours.\"}",
                symbol, price, timestamp
            );
            
            // Tool execution completed (logged only)
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("✅ getStockPrice completed in {}ms for {}", duration, symbol);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error getting stock price for {}: {}", symbol, e.getMessage(), e);
            
            // Error logged
            if (sessionId != null) {
                log.debug("❌ getStockPrice failed for {}: {}", symbol, e.getMessage());
            }
            
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error fetching stock price: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Get stock price data for a time period. Use this to analyze price trends over time. " +
          "Requires: symbol (string), timeframe (string: 'daily', 'weekly', or 'monthly'). " +
          "Returns price data with high, low, average, and time series data.")
    public String getStockPriceData(String symbol, String timeframe) {
        log.info("🔵 getStockPriceData CALLED with symbol={}, timeframe={}", symbol, timeframe);
        try {
            Map<String, Object> data = marketDataService.getStockPriceData(symbol, timeframe);
            if (data.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to fetch price data.\"}", symbol, timeframe);
            }
            
            // Convert to JSON string for LLM consumption
            String json = objectMapper.writeValueAsString(data);
            return json;
        } catch (Exception e) {
            log.error("Error getting stock price data for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Error fetching price data: %s\"}", symbol, timeframe, e.getMessage());
        }
    }

    @Tool("Get market news and sentiment for a stock symbol. Use this to understand recent news, events, and market sentiment. " +
          "Requires: symbol (string). Returns recent news headlines and sentiment information.")
    public String getMarketNews(String symbol) {
        log.info("🔵 getMarketNews CALLED with symbol={}", symbol);
        try {
            String news = marketDataService.getMarketNews(symbol);
            return String.format("{\"symbol\": \"%s\", \"news\": \"%s\"}", symbol, news);
        } catch (Exception e) {
            log.error("Error getting market news for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error fetching news: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Analyze price trends for a stock. Use this to determine if the stock is in an uptrend, downtrend, or sideways. " +
          "Requires: symbol (string), timeframe (string: 'daily', 'weekly', or 'monthly'). " +
          "Returns trend analysis with direction, strength, and key price levels.")
    public String analyzeTrends(String symbol, String timeframe) {
        log.info("🔵 analyzeTrends CALLED with symbol={}, timeframe={}", symbol, timeframe);
        try {
            Map<String, Object> priceData = marketDataService.getStockPriceData(symbol, timeframe);
            if (priceData.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to analyze trends - no price data available.\"}", symbol, timeframe);
            }

            // Simple trend analysis
            BigDecimal high = (BigDecimal) priceData.get("high");
            BigDecimal low = (BigDecimal) priceData.get("low");
            BigDecimal average = (BigDecimal) priceData.get("average");
            
            if (high == null || low == null || average == null) {
                return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Insufficient data for trend analysis.\"}", symbol, timeframe);
            }

            // Calculate trend direction (simplified - comparing high/low to average)
            String trendDirection = "SIDEWAYS";
            BigDecimal range = high.subtract(low);
            BigDecimal rangePercent = range.divide(average, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            if (rangePercent.compareTo(BigDecimal.valueOf(5)) > 0) {
                // Significant price movement
                BigDecimal currentPrice = marketDataService.getStockPrice(symbol);
                if (currentPrice != null) {
                    if (currentPrice.compareTo(average) > 0) {
                        trendDirection = "UPTREND";
                    } else {
                        trendDirection = "DOWNTREND";
                    }
                }
            }

            return String.format(
                "{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"trend\": \"%s\", " +
                "\"high\": %s, \"low\": %s, \"average\": %s, \"volatility\": \"%.2f%%\", " +
                "\"message\": \"Trend analysis completed\"}",
                symbol, timeframe, trendDirection, high, low, average, rangePercent.doubleValue()
            );
        } catch (Exception e) {
            log.error("Error analyzing trends for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Error analyzing trends: %s\"}", symbol, timeframe, e.getMessage());
        }
    }

    @Tool("Get technical indicators for a stock. Use this to get technical analysis metrics like moving averages, RSI, etc. " +
          "Requires: symbol (string). Returns technical indicators and their interpretations.")
    public String getTechnicalIndicators(String symbol) {
        log.info("🔵 getTechnicalIndicators CALLED with symbol={}", symbol);
        try {
            // Get price data for calculation
            Map<String, Object> dailyData = marketDataService.getStockPriceData(symbol, "daily");
            BigDecimal currentPrice = marketDataService.getStockPrice(symbol);
            
            if (dailyData.isEmpty() || currentPrice == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to calculate technical indicators - insufficient data.\"}", symbol);
            }

            BigDecimal high = (BigDecimal) dailyData.get("high");
            BigDecimal low = (BigDecimal) dailyData.get("low");
            BigDecimal average = (BigDecimal) dailyData.get("average");

            if (high == null || low == null || average == null) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Insufficient data for technical indicators.\"}", symbol);
            }

            // Simple technical indicators
            BigDecimal priceChange = currentPrice.subtract(average);
            BigDecimal priceChangePercent = priceChange.divide(average, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            String signal = "NEUTRAL";
            if (priceChangePercent.compareTo(BigDecimal.valueOf(5)) > 0) {
                signal = "BULLISH";
            } else if (priceChangePercent.compareTo(BigDecimal.valueOf(-5)) < 0) {
                signal = "BEARISH";
            }

            return String.format(
                "{\"symbol\": \"%s\", \"currentPrice\": %s, \"averagePrice\": %s, " +
                "\"priceChange\": \"%.2f%%\", \"signal\": \"%s\", " +
                "\"supportLevel\": %s, \"resistanceLevel\": %s, " +
                "\"message\": \"Technical indicators calculated\"}",
                symbol, currentPrice, average, priceChangePercent.doubleValue(), signal, low, high
            );
        } catch (Exception e) {
            log.error("Error getting technical indicators for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error calculating technical indicators: %s\"}", symbol, e.getMessage());
        }
    }
}

