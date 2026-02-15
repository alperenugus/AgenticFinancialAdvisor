package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.aspect.ToolCallAspect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fintwit Analysis Agent - Analyzes financial Twitter sentiment and trends
 * Uses Twitter API v2 if available, otherwise falls back to web search
 */
@Service
public class FintwitAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(FintwitAnalysisAgent.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final WebSearchAgent webSearchAgent; // Fallback for finding fintwit content
    private final String twitterBearerToken;
    private final String twitterApiBaseUrl;
    private final boolean useTwitterApi;

    @Autowired
    public FintwitAnalysisAgent(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            WebSearchAgent webSearchAgent,
            @Value("${fintwit.twitter.bearer-token:}") String twitterBearerToken,
            @Value("${fintwit.twitter.base-url:https://api.twitter.com/2}") String twitterApiBaseUrl
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.webSearchAgent = webSearchAgent;
        this.twitterBearerToken = twitterBearerToken;
        this.twitterApiBaseUrl = twitterApiBaseUrl;
        this.useTwitterApi = twitterBearerToken != null && !twitterBearerToken.trim().isEmpty();
    }

    @Tool("Get financial Twitter sentiment for a stock. Returns sentiment analysis and trending discussions. " +
          "Requires: symbol (string, e.g., 'AAPL', 'NVDA').")
    public String getFintwitSentiment(String symbol) {
        log.info("🔵 [FINTWIT_AGENT] getFintwitSentiment CALLED with symbol={}", symbol);
        String sessionId = ToolCallAspect.getSessionId();
        long startTime = System.currentTimeMillis();
        try {
            String result;
            if (useTwitterApi) {
                result = getTwitterSentiment(symbol);
            } else {
                // Fallback: Use web search to find fintwit content
                result = getFintwitSentimentViaWebSearch(symbol);
            }
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ [FINTWIT_AGENT] getFintwitSentiment completed in {}ms", duration);
                String responsePreview = result.length() > 500 ? result.substring(0, 500) + "..." : result;
                log.info("📥 [FINTWIT_AGENT] Response preview: {}", responsePreview);
            }
            return result;
        } catch (Exception e) {
            log.error("❌ [FINTWIT_AGENT] Error getting fintwit sentiment: {}", e.getMessage(), e);
            String errorResponse = String.format("{\"symbol\": \"%s\", \"error\": \"Error getting fintwit sentiment: %s\"}", symbol, e.getMessage());
            if (sessionId != null) {
                log.info("📥 [FINTWIT_AGENT] Error response: {}", errorResponse);
            }
            return errorResponse;
        }
    }

    @Tool("Get trending financial topics on Twitter. Returns trending topics and discussion summaries. " +
          "Requires: query (string, e.g., 'AI stocks' or 'market outlook').")
    public String getFintwitTrends(String query) {
        log.info("🔵 getFintwitTrends CALLED with query={}", query);
        try {
            if (useTwitterApi) {
                return getTwitterTrends(query);
            } else {
                // Fallback: Use web search
                return getFintwitTrendsViaWebSearch(query);
            }
        } catch (Exception e) {
            log.error("Error getting fintwit trends: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting fintwit trends: %s\"}", e.getMessage());
        }
    }

    @Tool("Analyze Twitter mentions and sentiment for a stock. Returns detailed sentiment analysis. " +
          "Requires: symbol (string, e.g., 'AAPL', 'NVDA').")
    public String analyzeFintwitMentions(String symbol) {
        log.info("🔵 analyzeFintwitMentions CALLED with symbol={}", symbol);
        try {
            if (useTwitterApi) {
                return analyzeTwitterMentions(symbol);
            } else {
                // Fallback: Use web search
                return analyzeMentionsViaWebSearch(symbol);
            }
        } catch (Exception e) {
            log.error("Error analyzing fintwit mentions: {}", e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error analyzing mentions: %s\"}", symbol, e.getMessage());
        }
    }

    /**
     * Get sentiment using Twitter API v2
     */
    private String getTwitterSentiment(String symbol) {
        try {
            // Search for tweets about the symbol
            String query = symbol + " stock OR $" + symbol;
            String url = twitterApiBaseUrl + "/tweets/search/recent?query=" + 
                        java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) +
                        "&max_results=10&tweet.fields=created_at,public_metrics,text";

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + twitterBearerToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode json = objectMapper.readTree(response);

            // Analyze sentiment (simplified - in production, use proper sentiment analysis)
            int positiveCount = 0;
            int negativeCount = 0;
            int neutralCount = 0;
            StringBuilder discussions = new StringBuilder();

            if (json.has("data")) {
                for (JsonNode tweet : json.get("data")) {
                    String text = tweet.has("text") ? tweet.get("text").asText() : "";
                    discussions.append(text).append("\n");
                    
                    // Simple sentiment analysis (in production, use proper NLP)
                    String lowerText = text.toLowerCase();
                    if (lowerText.contains("bull") || lowerText.contains("buy") || lowerText.contains("moon")) {
                        positiveCount++;
                    } else if (lowerText.contains("bear") || lowerText.contains("sell") || lowerText.contains("crash")) {
                        negativeCount++;
                    } else {
                        neutralCount++;
                    }
                }
            }

            String overallSentiment = "NEUTRAL";
            if (positiveCount > negativeCount && positiveCount > neutralCount) {
                overallSentiment = "BULLISH";
            } else if (negativeCount > positiveCount && negativeCount > neutralCount) {
                overallSentiment = "BEARISH";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("overallSentiment", overallSentiment);
            result.put("positiveCount", positiveCount);
            result.put("negativeCount", negativeCount);
            result.put("neutralCount", neutralCount);
            result.put("totalMentions", positiveCount + negativeCount + neutralCount);
            result.put("discussions", discussions.toString());
            result.put("source", "Twitter API");

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error getting Twitter sentiment: {}", e.getMessage(), e);
            // Fallback to web search
            return getFintwitSentimentViaWebSearch(symbol);
        }
    }

    /**
     * Fallback: Get fintwit sentiment via web search
     */
    private String getFintwitSentimentViaWebSearch(String symbol) {
        try {
            // Search for fintwit content about the symbol
            String searchQuery = symbol + " fintwit twitter sentiment stock";
            String searchResults = webSearchAgent.searchFinancialNews(searchQuery);
            
            JsonNode searchJson = objectMapper.readTree(searchResults);
            
            // Extract and analyze sentiment from search results
            StringBuilder content = new StringBuilder();
            if (searchJson.has("results")) {
                for (JsonNode result : searchJson.get("results")) {
                    if (result.has("content")) {
                        content.append(result.get("content").asText()).append("\n");
                    } else if (result.has("snippet")) {
                        content.append(result.get("snippet").asText()).append("\n");
                    }
                }
            }

            // Simple sentiment analysis
            String contentStr = content.toString().toLowerCase();
            int bullish = (contentStr.split("bull|buy|moon|up|positive").length - 1);
            int bearish = (contentStr.split("bear|sell|crash|down|negative").length - 1);
            
            String sentiment = bullish > bearish ? "BULLISH" : (bearish > bullish ? "BEARISH" : "NEUTRAL");

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("overallSentiment", sentiment);
            result.put("bullishIndicators", bullish);
            result.put("bearishIndicators", bearish);
            result.put("content", content.toString());
            result.put("source", "Web Search (fintwit content)");
            result.put("note", "Using web search to find fintwit content. For direct Twitter API access, set TWITTER_BEARER_TOKEN.");

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error getting fintwit sentiment via web search: {}", e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error getting fintwit sentiment: %s\"}", symbol, e.getMessage());
        }
    }

    /**
     * Get Twitter trends
     */
    private String getTwitterTrends(String query) {
        // Similar implementation to getTwitterSentiment but for trends
        // For now, fallback to web search
        return getFintwitTrendsViaWebSearch(query);
    }

    /**
     * Fallback: Get trends via web search
     */
    private String getFintwitTrendsViaWebSearch(String query) {
        try {
            String searchQuery = query + " fintwit twitter trending financial";
            String searchResults = webSearchAgent.searchMarketTrends(searchQuery);
            
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("searchResults", searchResults);
            result.put("source", "Web Search");
            result.put("note", "Using web search to find fintwit trends. For direct Twitter API access, set TWITTER_BEARER_TOKEN.");

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error getting fintwit trends: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting fintwit trends: %s\"}", e.getMessage());
        }
    }

    /**
     * Analyze Twitter mentions
     */
    private String analyzeTwitterMentions(String symbol) {
        // Similar to getTwitterSentiment but with more detailed analysis
        return getTwitterSentiment(symbol);
    }

    /**
     * Fallback: Analyze mentions via web search
     */
    private String analyzeMentionsViaWebSearch(String symbol) {
        return getFintwitSentimentViaWebSearch(symbol);
    }
}

