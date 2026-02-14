package com.agent.financialadvisor.service.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Search Agent - Provides web search capabilities for financial data
 * Uses Tavily API (purpose-built for LLM agents) or Serper API as fallback
 */
@Service
public class WebSearchAgent {

    private static final Logger log = LoggerFactory.getLogger(WebSearchAgent.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String tavilyApiKey;
    private final String tavilyBaseUrl;
    private final String serperApiKey;
    private final String serperBaseUrl;
    private final boolean useTavily;

    public WebSearchAgent(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${web-search.tavily.api-key:}") String tavilyApiKey,
            @Value("${web-search.tavily.base-url:https://api.tavily.com}") String tavilyBaseUrl,
            @Value("${web-search.serper.api-key:}") String serperApiKey,
            @Value("${web-search.serper.base-url:https://google.serper.dev}") String serperBaseUrl
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.tavilyApiKey = tavilyApiKey;
        this.tavilyBaseUrl = tavilyBaseUrl;
        this.serperApiKey = serperApiKey;
        this.serperBaseUrl = serperBaseUrl;
        // Prefer Tavily if API key is available, otherwise use Serper
        this.useTavily = tavilyApiKey != null && !tavilyApiKey.trim().isEmpty();
    }

    @Tool("Search the web for financial news, analysis, and market insights. " +
          "Use this to get the latest information about stocks, companies, or market trends. " +
          "Requires: query (string, e.g., 'NVDA stock analysis 2024' or 'Apple earnings report'). " +
          "Returns relevant search results with summaries and sources.")
    public String searchFinancialNews(String query) {
        log.info("🔵 searchFinancialNews CALLED with query={}", query);
        try {
            // Enhance query for financial context
            String enhancedQuery = query + " financial news market analysis";
            return performSearch(enhancedQuery, 5);
        } catch (Exception e) {
            log.error("Error searching financial news: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error searching financial news: %s\"}", e.getMessage());
        }
    }

    @Tool("Search for stock analysis and research reports. " +
          "Use this to find professional analysis, price targets, and investment research for a stock. " +
          "Requires: symbol (string, e.g., 'AAPL', 'NVDA'). " +
          "Returns analysis, price targets, and research summaries.")
    public String searchStockAnalysis(String symbol) {
        log.info("🔵 searchStockAnalysis CALLED with symbol={}", symbol);
        try {
            String query = symbol + " stock analysis price target investment research 2024";
            return performSearch(query, 5);
        } catch (Exception e) {
            log.error("Error searching stock analysis: {}", e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error searching stock analysis: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Search for market trends and sector analysis. " +
          "Use this to understand broader market trends, sector performance, or economic indicators. " +
          "Requires: query (string, e.g., 'AI sector trends 2024' or 'market outlook January 2024'). " +
          "Returns market trend analysis and insights.")
    public String searchMarketTrends(String query) {
        log.info("🔵 searchMarketTrends CALLED with query={}", query);
        try {
            String enhancedQuery = query + " market trends sector analysis";
            return performSearch(enhancedQuery, 5);
        } catch (Exception e) {
            log.error("Error searching market trends: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error searching market trends: %s\"}", e.getMessage());
        }
    }

    @Tool("Search for company information, earnings, and corporate news. " +
          "Use this to find latest company news, earnings reports, and corporate developments. " +
          "Requires: symbol (string, e.g., 'AAPL', 'MSFT'). " +
          "Returns company information, earnings, and corporate news.")
    public String searchCompanyInfo(String symbol) {
        log.info("🔵 searchCompanyInfo CALLED with symbol={}", symbol);
        try {
            String query = symbol + " company news earnings report corporate developments 2024";
            return performSearch(query, 5);
        } catch (Exception e) {
            log.error("Error searching company info: {}", e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error searching company info: %s\"}", symbol, e.getMessage());
        }
    }

    /**
     * Perform web search using Tavily or Serper API
     */
    private String performSearch(String query, int maxResults) {
        if (useTavily && tavilyApiKey != null && !tavilyApiKey.trim().isEmpty()) {
            return searchWithTavily(query, maxResults);
        } else if (serperApiKey != null && !serperApiKey.trim().isEmpty()) {
            return searchWithSerper(query, maxResults);
        } else {
            log.warn("No web search API key configured. Please set TAVILY_API_KEY or SERPER_API_KEY");
            return String.format("{\"error\": \"Web search not configured. Please set TAVILY_API_KEY or SERPER_API_KEY environment variable.\"}");
        }
    }

    /**
     * Search using Tavily API (purpose-built for LLM agents)
     */
    private String searchWithTavily(String query, int maxResults) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", tavilyApiKey);
            requestBody.put("query", query);
            requestBody.put("search_depth", "basic"); // or "advanced" for deeper search
            requestBody.put("include_answer", true);
            requestBody.put("include_images", false);
            requestBody.put("include_raw_content", false);
            requestBody.put("max_results", maxResults);

            String response = webClient.post()
                    .uri(tavilyBaseUrl + "/search")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode json = objectMapper.readTree(response);

            // Extract results
            List<Map<String, Object>> results = new ArrayList<>();
            if (json.has("results")) {
                for (JsonNode result : json.get("results")) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("title", result.has("title") ? result.get("title").asText() : "");
                    resultMap.put("url", result.has("url") ? result.get("url").asText() : "");
                    resultMap.put("content", result.has("content") ? result.get("content").asText() : "");
                    results.add(resultMap);
                }
            }

            // Extract answer if available
            String answer = json.has("answer") ? json.get("answer").asText() : "";

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("query", query);
            responseMap.put("answer", answer);
            responseMap.put("results", results);
            responseMap.put("count", results.size());
            responseMap.put("source", "Tavily");

            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            log.error("Error searching with Tavily: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error searching with Tavily: %s\"}", e.getMessage());
        }
    }

    /**
     * Search using Serper API (Google Search)
     */
    private String searchWithSerper(String query, int maxResults) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", query);
            requestBody.put("num", maxResults);

            String response = webClient.post()
                    .uri(serperBaseUrl + "/search")
                    .header("X-API-KEY", serperApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode json = objectMapper.readTree(response);

            // Extract organic results
            List<Map<String, Object>> results = new ArrayList<>();
            if (json.has("organic")) {
                for (JsonNode result : json.get("organic")) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("title", result.has("title") ? result.get("title").asText() : "");
                    resultMap.put("url", result.has("link") ? result.get("link").asText() : "");
                    resultMap.put("snippet", result.has("snippet") ? result.get("snippet").asText() : "");
                    results.add(resultMap);
                }
            }

            // Extract answer box if available
            String answer = "";
            if (json.has("answerBox") && json.get("answerBox").has("answer")) {
                answer = json.get("answerBox").get("answer").asText();
            }

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("query", query);
            responseMap.put("answer", answer);
            responseMap.put("results", results);
            responseMap.put("count", results.size());
            responseMap.put("source", "Serper");

            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            log.error("Error searching with Serper: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error searching with Serper: %s\"}", e.getMessage());
        }
    }
}

