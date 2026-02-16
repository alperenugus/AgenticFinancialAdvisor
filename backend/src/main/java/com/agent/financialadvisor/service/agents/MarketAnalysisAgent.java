package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.aspect.ToolCallAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(MarketAnalysisAgent.class);
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;
    private final ChatLanguageModel chatLanguageModel;
    private final Map<String, MarketAnalysisAgentService> agentCache = new ConcurrentHashMap<>();

    @Autowired
    public MarketAnalysisAgent(
            MarketDataService marketDataService, 
            ObjectMapper objectMapper,
            WebSocketService webSocketService,
            @Qualifier("agentChatLanguageModel") ChatLanguageModel chatLanguageModel
    ) {
        this.marketDataService = marketDataService;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
        this.chatLanguageModel = chatLanguageModel;
        log.info("✅ MarketAnalysisAgent initialized with its own LLM instance");
    }

    /**
     * Get or create an AI agent service instance for a session
     * Note: No chat memory - each query is independent
     */
    private MarketAnalysisAgentService getOrCreateAgentService(String sessionId) {
        return agentCache.computeIfAbsent(sessionId, sid -> {
            log.info("Creating new MarketAnalysisAgent AI service for session: {}", sid);
            
            return AiServices.builder(MarketAnalysisAgentService.class)
                    .chatLanguageModel(chatLanguageModel)
                    .tools(this)  // This agent's tools
                    .build();
        });
    }

    /**
     * Process a query using this agent's LLM
     */
    public String processQuery(String sessionId, String query) {
        MarketAnalysisAgentService agent = getOrCreateAgentService(sessionId);
        return agent.chat(sessionId, query);
    }

    /**
     * AI Agent Service interface for MarketAnalysisAgent
     */
    private interface MarketAnalysisAgentService {
        @SystemMessage("You are a Market Analysis Agent. " +
                "Your role is to analyze stock prices, market data, technical indicators, and price trends. " +
                "You have access to tools for getting stock prices, price data, market news, technical indicators, and trend analysis. " +
                "When asked about stock prices, market data, or technical analysis, use the appropriate tools. " +
                "If user names a company (not ticker), pass that exact company name to tools and let tools resolve the ticker using live data. " +
                "Never substitute a different company (for example, parent/subsidiary) from memory. " +
                "ALWAYS use tools to get current data - your training data is outdated. " +
                "Provide accurate, data-driven analysis based on real-time market information. " +
                "If tool data is unavailable, explicitly say data is unavailable instead of guessing. " +
                "Keep answers concise unless user asks for deep detail.")
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    @Tool("Get current stock price for a ticker OR company name. ALWAYS use this tool for stock prices - your training data is outdated. " +
          "Requires: symbolOrCompany (string, e.g., 'AAPL', 'TSLA', 'Figma'). Tool performs live symbol resolution and returns current price with timestamp.")
    public String getStockPrice(String symbol) {
        log.info("🔵 getStockPrice CALLED with symbol={} - FETCHING FRESH DATA FROM API", symbol);
        
        // Tool call tracking (for logging only)
        String sessionId = ToolCallAspect.getSessionId();
        
        long startTime = System.currentTimeMillis();
        try {
            String requestedInput = symbol == null ? "" : symbol.trim();
            String resolvedSymbol = marketDataService.resolveSymbol(requestedInput);
            if (resolvedSymbol == null || resolvedSymbol.isBlank()) {
                return String.format(
                        "{\"requested\": \"%s\", \"error\": \"Unable to resolve a tradable ticker from the provided company/symbol. Please provide a valid ticker or company name.\"}",
                        escapeJson(requestedInput)
                );
            }

            BigDecimal price = marketDataService.getStockPrice(resolvedSymbol);
            if (price == null) {
                return String.format(
                        "{\"requested\": \"%s\", \"symbol\": \"%s\", \"error\": \"Unable to fetch stock price. Symbol may be invalid, newly listed but unavailable, or API limit reached.\"}",
                        escapeJson(requestedInput), resolvedSymbol
                );
            }
            // Include timestamp to show data freshness
            String timestamp = java.time.LocalDateTime.now().toString();
            String result;
            if (requestedInput.equalsIgnoreCase(resolvedSymbol)) {
                result = String.format(
                    "{\"symbol\": \"%s\", \"price\": %s, \"currency\": \"USD\", \"fetchedAt\": \"%s\", " +
                    "\"note\": \"Fresh data fetched from API. Free tier may have 15-minute delay during market hours.\"}",
                    resolvedSymbol, price, timestamp
                );
            } else {
                result = String.format(
                    "{\"requested\": \"%s\", \"symbol\": \"%s\", \"price\": %s, \"currency\": \"USD\", \"fetchedAt\": \"%s\", " +
                    "\"note\": \"Fresh data fetched from API after live symbol resolution. Free tier may have 15-minute delay during market hours.\"}",
                    escapeJson(requestedInput), resolvedSymbol, price, timestamp
                );
            }
            
            // Log response
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ [MARKET_AGENT] getStockPrice completed in {}ms for {} (resolved={})", duration, symbol, resolvedSymbol);
                log.info("📥 [MARKET_AGENT] Response: {}", result);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error getting stock price for {}: {}", symbol, e.getMessage(), e);
            
            // Error logged
            String errorResponse = String.format("{\"symbol\": \"%s\", \"error\": \"Error fetching stock price: %s\"}", symbol, e.getMessage());
            if (sessionId != null) {
                log.error("❌ [MARKET_AGENT] getStockPrice failed for {}: {}", symbol, e.getMessage());
                log.info("📥 [MARKET_AGENT] Error response: {}", errorResponse);
            }
            
            return errorResponse;
        }
    }

    @Tool("Get stock price data for a time period. Use this to analyze price trends. " +
          "Requires: symbolOrCompany (string), timeframe ('daily', 'weekly', or 'monthly'). Returns price data with high, low, average.")
    public String getStockPriceData(String symbol, String timeframe) {
        log.info("🔵 getStockPriceData CALLED with symbol={}, timeframe={}", symbol, timeframe);
        try {
            String resolvedSymbol = marketDataService.resolveSymbol(symbol);
            if (resolvedSymbol == null || resolvedSymbol.isBlank()) {
                return String.format(
                        "{\"requested\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to resolve a tradable ticker from the provided company/symbol.\"}",
                        escapeJson(symbol), timeframe
                );
            }

            Map<String, Object> data = marketDataService.getStockPriceData(resolvedSymbol, timeframe);
            if (data.isEmpty()) {
                return String.format("{\"requested\": \"%s\", \"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to fetch price data.\"}",
                        escapeJson(symbol), resolvedSymbol, timeframe);
            }

            if (symbol != null && !symbol.trim().equalsIgnoreCase(resolvedSymbol)) {
                data.put("requested", symbol.trim());
            }
            
            // Convert to JSON string for LLM consumption
            String json = objectMapper.writeValueAsString(data);
            return json;
        } catch (Exception e) {
            log.error("Error getting stock price data for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Error fetching price data: %s\"}", symbol, timeframe, e.getMessage());
        }
    }

    @Tool("Get market news and sentiment for a stock. Returns recent news headlines and sentiment. " +
          "Requires: symbolOrCompany (string).")
    public String getMarketNews(String symbol) {
        log.info("🔵 getMarketNews CALLED with symbol={}", symbol);
        try {
            String resolvedSymbol = marketDataService.resolveSymbol(symbol);
            if (resolvedSymbol == null || resolvedSymbol.isBlank()) {
                return String.format(
                        "{\"requested\": \"%s\", \"error\": \"Unable to resolve a tradable ticker from the provided company/symbol.\"}",
                        escapeJson(symbol)
                );
            }

            String news = marketDataService.getMarketNews(resolvedSymbol);
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("symbol", resolvedSymbol);
            if (symbol != null && !symbol.trim().equalsIgnoreCase(resolvedSymbol)) {
                payload.put("requested", symbol.trim());
            }
            payload.put("news", news);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error getting market news for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error fetching news: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Analyze price trends for a stock. Determines uptrend, downtrend, or sideways movement. " +
          "Requires: symbolOrCompany (string), timeframe ('daily', 'weekly', or 'monthly'). Returns trend direction and key levels.")
    public String analyzeTrends(String symbol, String timeframe) {
        log.info("🔵 analyzeTrends CALLED with symbol={}, timeframe={}", symbol, timeframe);
        try {
            String resolvedSymbol = marketDataService.resolveSymbol(symbol);
            if (resolvedSymbol == null || resolvedSymbol.isBlank()) {
                return String.format(
                        "{\"requested\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to resolve a tradable ticker from the provided company/symbol.\"}",
                        escapeJson(symbol), timeframe
                );
            }

            Map<String, Object> priceData = marketDataService.getStockPriceData(resolvedSymbol, timeframe);
            if (priceData.isEmpty()) {
                return String.format("{\"requested\": \"%s\", \"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Unable to analyze trends - no price data available.\"}",
                        escapeJson(symbol), resolvedSymbol, timeframe);
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
                BigDecimal currentPrice = marketDataService.getStockPrice(resolvedSymbol);
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
                resolvedSymbol, timeframe, trendDirection, high, low, average, rangePercent.doubleValue()
            );
        } catch (Exception e) {
            log.error("Error analyzing trends for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"timeframe\": \"%s\", \"error\": \"Error analyzing trends: %s\"}", symbol, timeframe, e.getMessage());
        }
    }

    @Tool("Get technical indicators for a stock. Returns technical analysis metrics. " +
          "Requires: symbolOrCompany (string).")
    public String getTechnicalIndicators(String symbol) {
        log.info("🔵 getTechnicalIndicators CALLED with symbol={}", symbol);
        try {
            String resolvedSymbol = marketDataService.resolveSymbol(symbol);
            if (resolvedSymbol == null || resolvedSymbol.isBlank()) {
                return String.format(
                        "{\"requested\": \"%s\", \"error\": \"Unable to resolve a tradable ticker from the provided company/symbol.\"}",
                        escapeJson(symbol)
                );
            }

            // Get price data for calculation
            Map<String, Object> dailyData = marketDataService.getStockPriceData(resolvedSymbol, "daily");
            BigDecimal currentPrice = marketDataService.getStockPrice(resolvedSymbol);
            
            if (dailyData.isEmpty() || currentPrice == null) {
                return String.format(
                        "{\"requested\": \"%s\", \"symbol\": \"%s\", \"error\": \"Unable to calculate technical indicators - insufficient data.\"}",
                        escapeJson(symbol), resolvedSymbol
                );
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
                resolvedSymbol, currentPrice, average, priceChangePercent.doubleValue(), signal, low, high
            );
        } catch (Exception e) {
            log.error("Error getting technical indicators for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error calculating technical indicators: %s\"}", symbol, e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

