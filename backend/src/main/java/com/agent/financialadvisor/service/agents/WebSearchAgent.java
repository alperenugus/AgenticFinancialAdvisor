package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.aspect.ToolCallAspect;
import com.agent.financialadvisor.service.WebSocketService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Search Agent - Provides web search capabilities for financial data
 * Uses Tavily MCP (Model Context Protocol) or Serper API as fallback
 */
@Service
public class WebSearchAgent {

    private static final Logger log = LoggerFactory.getLogger(WebSearchAgent.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;
    private final String tavilyApiKey;
    private final String tavilyMcpUrl;
    private final String serperApiKey;
    private final String serperBaseUrl;
    private final boolean useTavily;

    @Autowired
    public WebSearchAgent(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            WebSocketService webSocketService,
            @Value("${web-search.tavily.api-key:}") String tavilyApiKey,
            @Value("${web-search.tavily.mcp-url:https://mcp.tavily.com/mcp/}") String tavilyMcpUrl,
            @Value("${web-search.serper.api-key:}") String serperApiKey,
            @Value("${web-search.serper.base-url:https://google.serper.dev}") String serperBaseUrl
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
        this.tavilyApiKey = tavilyApiKey;
        // Build MCP URL with API key if provided
        if (tavilyApiKey != null && !tavilyApiKey.trim().isEmpty()) {
            this.tavilyMcpUrl = tavilyMcpUrl + "?tavilyApiKey=" + tavilyApiKey;
        } else {
            this.tavilyMcpUrl = tavilyMcpUrl;
        }
        this.serperApiKey = serperApiKey;
        this.serperBaseUrl = serperBaseUrl;
        // Prefer Tavily if API key is available, otherwise use Serper
        this.useTavily = tavilyApiKey != null && !tavilyApiKey.trim().isEmpty();
        
        if (useTavily) {
            log.info("✅ Using Tavily MCP at: {}", this.tavilyMcpUrl);
        }
    }

    @Tool("Search the web for financial news, analysis, and market insights. " +
          "Use this to get the latest information about stocks, companies, or market trends. " +
          "Requires: query (string, e.g., 'NVDA stock analysis 2024' or 'Apple earnings report'). " +
          "Returns relevant search results with summaries and sources.")
    public String searchFinancialNews(String query) {
        log.info("🔵 searchFinancialNews CALLED with query={}", query);
        
        String sessionId = ToolCallAspect.getSessionId();
        if (sessionId != null) {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("query", query);
            webSocketService.sendToolCall(sessionId, "Search Financial News", params);
            webSocketService.sendReasoning(sessionId, "🔍 Searching web for financial news: " + query);
        }
        
        long startTime = System.currentTimeMillis();
        try {
            // Enhance query for financial context
            String enhancedQuery = query + " financial news market analysis";
            String result = performSearch(enhancedQuery, 5);
            
            if (sessionId != null) {
                long duration = System.currentTimeMillis() - startTime;
                webSocketService.sendToolResult(sessionId, "Search Financial News", 
                    "Found search results", duration);
                webSocketService.sendReasoning(sessionId, "✅ Retrieved financial news search results");
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error searching financial news: {}", e.getMessage(), e);
            if (sessionId != null) {
                webSocketService.sendReasoning(sessionId, "❌ Failed to search financial news: " + e.getMessage());
            }
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
     * Search using Tavily MCP (Model Context Protocol)
     * MCP uses JSON-RPC 2.0 protocol
     */
    private String searchWithTavily(String query, int maxResults) {
        try {
            // MCP uses JSON-RPC 2.0 format
            Map<String, Object> rpcRequest = new HashMap<>();
            rpcRequest.put("jsonrpc", "2.0");
            rpcRequest.put("id", System.currentTimeMillis());
            rpcRequest.put("method", "tools/call");
            
            // Build parameters for Tavily search
            Map<String, Object> params = new HashMap<>();
            params.put("name", "tavily_search_results_json");
            
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("query", query);
            arguments.put("max_results", maxResults);
            arguments.put("search_depth", "basic");
            arguments.put("include_answer", true);
            arguments.put("include_images", false);
            arguments.put("include_raw_content", false);
            
            params.put("arguments", arguments);
            rpcRequest.put("params", params);

            log.info("🔍 Calling Tavily MCP with query: {}", query);
            
            String response = webClient.post()
                    .uri(tavilyMcpUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(rpcRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.debug("Tavily MCP response: {}", response);
            JsonNode json = objectMapper.readTree(response);

            // Handle JSON-RPC response
            if (json.has("error")) {
                JsonNode errorNode = json.get("error");
                String errorMsg = errorNode.has("message") ? errorNode.get("message").asText() : errorNode.toString();
                log.error("Tavily MCP error: {}", errorMsg);
                return String.format("{\"error\": \"Tavily MCP error: %s\"}", errorMsg);
            }

            // Extract result from JSON-RPC response
            JsonNode resultNode = json.has("result") ? json.get("result") : null;
            if (resultNode == null) {
                log.warn("Tavily MCP response missing 'result' field: {}", response);
                return String.format("{\"error\": \"Tavily MCP response missing result field\"}");
            }

            // MCP response format: result.content[0].text contains the JSON string
            JsonNode contentArray = resultNode.has("content") ? resultNode.get("content") : null;
            if (contentArray == null || !contentArray.isArray() || contentArray.size() == 0) {
                log.warn("Tavily MCP response missing content array: {}", response);
                return String.format("{\"error\": \"Tavily MCP response missing content\"}");
            }

            // Parse the content (which should be a JSON string)
            String contentStr = contentArray.get(0).get("text").asText();
            JsonNode contentJson = objectMapper.readTree(contentStr);

            // Extract results
            List<Map<String, Object>> results = new ArrayList<>();
            if (contentJson.has("results")) {
                for (JsonNode result : contentJson.get("results")) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("title", result.has("title") ? result.get("title").asText() : "");
                    resultMap.put("url", result.has("url") ? result.get("url").asText() : "");
                    resultMap.put("content", result.has("content") ? result.get("content").asText() : "");
                    results.add(resultMap);
                }
            }

            // Extract answer if available
            String answer = contentJson.has("answer") ? contentJson.get("answer").asText() : "";

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("query", query);
            responseMap.put("answer", answer);
            responseMap.put("results", results);
            responseMap.put("count", results.size());
            responseMap.put("source", "Tavily MCP");

            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            log.error("Error searching with Tavily MCP: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error searching with Tavily MCP: %s\"}", e.getMessage());
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

