package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    
    private final ChatLanguageModel chatLanguageModel;
    private final UserProfileAgent userProfileAgent;
    private final MarketAnalysisAgent marketAnalysisAgent;
    private final RiskAssessmentAgent riskAssessmentAgent;
    private final ResearchAgent researchAgent;
    private final RecommendationAgent recommendationAgent;
    private final StockDiscoveryAgent stockDiscoveryAgent;
    private final WebSocketService webSocketService;
    private final int orchestratorTimeoutSeconds;
    
    // Cache for AI service instances per session
    private final Map<String, FinancialAdvisorAgent> agentCache = new HashMap<>();

    public OrchestratorService(
            ChatLanguageModel chatLanguageModel,
            UserProfileAgent userProfileAgent,
            MarketAnalysisAgent marketAnalysisAgent,
            RiskAssessmentAgent riskAssessmentAgent,
            ResearchAgent researchAgent,
            RecommendationAgent recommendationAgent,
            StockDiscoveryAgent stockDiscoveryAgent,
            WebSocketService webSocketService,
            @Value("${agent.timeout.orchestrator-seconds:60}") int orchestratorTimeoutSeconds
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.userProfileAgent = userProfileAgent;
        this.marketAnalysisAgent = marketAnalysisAgent;
        this.riskAssessmentAgent = riskAssessmentAgent;
        this.researchAgent = researchAgent;
        this.recommendationAgent = recommendationAgent;
        this.stockDiscoveryAgent = stockDiscoveryAgent;
        this.webSocketService = webSocketService;
        this.orchestratorTimeoutSeconds = orchestratorTimeoutSeconds;
    }

    /**
     * Main orchestration method - coordinates all agents to provide financial advice
     * Implements timeout handling to prevent hanging requests
     */
    public String coordinateAnalysis(String userId, String userQuery, String sessionId) {
        log.info("🎯 Orchestrator coordinating analysis for userId={}, query={}", userId, userQuery);
        
        try {
            // Send initial thinking update
            webSocketService.sendThinking(sessionId, "Analyzing your request and coordinating specialized agents...");

            // Get or create AI agent for this session
            FinancialAdvisorAgent agent = getOrCreateAgent(sessionId);

            // Build context-aware prompt
            String contextualQuery = buildContextualQuery(userId, userQuery);

            // Send thinking update
            webSocketService.sendThinking(sessionId, "Processing your request with AI agents...");

            // Execute the agent with timeout protection
            // Use CompletableFuture to enforce timeout limit
            CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
                try {
                    return agent.chat(sessionId, contextualQuery);
                } catch (Exception e) {
                    log.error("Error in agent chat execution: {}", e.getMessage(), e);
                    throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
                }
            });

            String response;
            try {
                // Wait for response with timeout
                response = futureResponse.get(orchestratorTimeoutSeconds, TimeUnit.SECONDS);
                
                // Send final response
                webSocketService.sendFinalResponse(sessionId, response);
                return response;
            } catch (TimeoutException e) {
                log.warn("Orchestrator timeout after {} seconds for sessionId={}, userId={}", 
                        orchestratorTimeoutSeconds, sessionId, userId);
                
                // Cancel the future to stop the agent execution
                futureResponse.cancel(true);
                
                String timeoutMsg = String.format(
                    "I apologize, but the analysis took longer than expected (exceeded %d seconds). " +
                    "This may be due to slow external API responses or high system load. " +
                    "Please try again with a simpler query, or try again later.",
                    orchestratorTimeoutSeconds
                );
                webSocketService.sendError(sessionId, timeoutMsg);
                return timeoutMsg;
            }
        } catch (Exception e) {
            log.error("Error in orchestration: {}", e.getMessage(), e);
            String errorMsg = "I apologize, but I encountered an error while processing your request. Please try again.";
            webSocketService.sendError(sessionId, errorMsg);
            return errorMsg;
        }
    }

    /**
     * Get or create an AI agent instance for a session
     */
    private FinancialAdvisorAgent getOrCreateAgent(String sessionId) {
        return agentCache.computeIfAbsent(sessionId, sid -> {
            log.info("Creating new AI agent for session: {}", sid);
            
            // Create a ChatMemoryProvider that provides MessageWindowChatMemory for each memory ID
            ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.withMaxMessages(20);
            
            return AiServices.builder(FinancialAdvisorAgent.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemoryProvider(memoryProvider)
                    .tools(
                            userProfileAgent,
                            marketAnalysisAgent,
                            riskAssessmentAgent,
                            researchAgent,
                            recommendationAgent,
                            stockDiscoveryAgent
                    )
                    .build();
        });
    }

    /**
     * Build a contextual query that includes user profile and portfolio information
     */
    private String buildContextualQuery(String userId, String userQuery) {
        StringBuilder contextualQuery = new StringBuilder();
        
        // Try to get user profile for context
        try {
            String profileInfo = userProfileAgent.getUserProfile(userId);
            if (profileInfo.contains("\"exists\": true")) {
                contextualQuery.append("User Profile Context: ").append(profileInfo).append("\n\n");
            } else {
                contextualQuery.append("Note: User profile not found. You may need to ask the user to create a profile first.\n\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile for context: {}", e.getMessage());
        }

        // Try to get user portfolio for context
        try {
            String portfolioInfo = userProfileAgent.getPortfolioSummary(userId);
            if (portfolioInfo.contains("\"exists\": true")) {
                contextualQuery.append("User Portfolio Context: ").append(portfolioInfo).append("\n\n");
            } else {
                contextualQuery.append("Note: User portfolio is empty or not found. User may not have any holdings yet.\n\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch user portfolio for context: {}", e.getMessage());
        }

        contextualQuery.append("User Query: ").append(userQuery);
        contextualQuery.append("\n\nPlease analyze this request and use the available tools to provide a comprehensive financial recommendation. ");
        contextualQuery.append("You have access to the user's profile and portfolio data. Use this information to provide personalized advice. ");
        contextualQuery.append("If the user is asking about a stock, analyze it thoroughly using market analysis, risk assessment, and research tools. ");
        contextualQuery.append("Consider how recommendations fit with their current portfolio and risk tolerance. ");
        contextualQuery.append("Then synthesize all information into a clear recommendation.");

        return contextualQuery.toString();
    }

    /**
     * AI Agent interface - defines the conversation interface
     */
    public interface FinancialAdvisorAgent {
        @SystemMessage("You are a PROFESSIONAL FINANCIAL ANALYST with deep expertise in technical analysis, portfolio management, and risk management. " +
                "You coordinate multiple specialized agents to provide comprehensive, professional-grade investment recommendations. " +
                "You have access to tools from: User Profile Agent (can access user profile AND portfolio), Market Analysis Agent, Risk Assessment Agent, Research Agent, Stock Discovery Agent, and Recommendation Agent. " +
                "IMPORTANT: You can access the user's portfolio using UserProfileAgent tools: getPortfolio(userId), getPortfolioHoldings(userId), and getPortfolioSummary(userId). " +
                "Always check the user's current portfolio before making recommendations to ensure advice is personalized and considers their existing holdings. " +
                "Always consider the user's risk tolerance, investment goals, and current portfolio when making recommendations. " +
                "When users greet you (hello, hi, hey, etc.), respond warmly and briefly introduce yourself as their financial advisor. " +
                "Then guide them to share their financial questions or goals. Do NOT immediately provide stock recommendations for greetings. " +
                "For greetings, say something like: 'Hello! I'm your AI financial advisor. I can help you with investment advice, portfolio analysis, and personalized recommendations. " +
                "What would you like to know about your investments today?' " +
                "Only provide investment recommendations when the user asks specific questions about stocks, their portfolio, or investment advice. " +
                "CRITICAL DATA FRESHNESS: All market data tools fetch FRESH, REAL-TIME data from external APIs. There is NO caching. " +
                "Every call to getStockPrice(), getStockPriceData(), etc. makes a fresh API request to get the latest available data. " +
                "Note: Free tier data may have a 15-minute delay during market hours. Premium tier provides real-time data. " +
                "CRITICAL: When providing recommendations, you MUST provide PROFESSIONAL FINANCIAL ANALYST-level analysis including: " +
                "1. Technical analysis patterns (head and shoulders, double tops/bottoms, triangles, support/resistance levels, etc.) " +
                "2. Stop loss levels (specific price points where to set stop losses, e.g., 'For this stock, you can have a stop loss at $X') " +
                "3. Averaging down advice (if applicable, e.g., 'For this stock, you can average down a bit if it drops to $X') " +
                "4. Entry and exit price suggestions " +
                "5. Target price with reasoning " +
                "6. Professional analysis of chart patterns, trends, and technical indicators " +
                "7. Portfolio management advice considering current holdings " +
                "MANDATORY: You MUST use the available tools to get REAL, CURRENT data. NEVER use placeholders like [$Current Price], [Stop Loss Price], [Current Date], etc. " +
                "ALWAYS call getStockPrice(symbol) FIRST to get the current price (fresh from API), then use that actual price in all calculations. " +
                "ALWAYS call getStockPriceData(symbol, timeframe) to get price history for technical analysis (fresh from API). " +
                "ALWAYS call analyzeTrends(symbol, timeframe) to identify chart patterns (uses fresh data). " +
                "Format your recommendations like a professional financial analyst would: " +
                "- 'For [SYMBOL], the current price is $X (fresh data from getStockPrice, fetched just now). I identify a [PATTERN] pattern on [TIMEFRAME] chart...' " +
                "- 'For [SYMBOL], you can have a stop loss at $Y (calculate Y as X * 0.95 or similar based on actual current price)...' " +
                "- 'For [SYMBOL], you can average down a bit if price reaches $Z (calculate Z based on actual current price)...' " +
                "- Provide specific price levels, percentages, and technical analysis using REAL, FRESH data from tools " +
                "Always use technical analysis tools (getStockPriceData, analyzeTrends, getTechnicalIndicators) to identify patterns and levels. " +
                "NEVER write placeholders - always fetch and use actual data. All data is fetched fresh from APIs with no caching. " +
                "Provide clear, well-reasoned, professional recommendations based on data from all agents. " +
                "If a user profile doesn't exist, guide them to create one first. " +
                "Always include appropriate disclaimers about investment risks, but only when giving actual investment advice.")
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    /**
     * Check agent status - verify all agents are available
     */
    public Map<String, Boolean> getAgentStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("userProfileAgent", userProfileAgent != null);
        status.put("marketAnalysisAgent", marketAnalysisAgent != null);
        status.put("riskAssessmentAgent", riskAssessmentAgent != null);
        status.put("researchAgent", researchAgent != null);
        status.put("recommendationAgent", recommendationAgent != null);
        status.put("chatLanguageModel", chatLanguageModel != null);
        return status;
    }

    /**
     * Clear agent cache for a session (useful for testing or session cleanup)
     */
    public void clearSession(String sessionId) {
        agentCache.remove(sessionId);
        log.info("Cleared agent cache for session: {}", sessionId);
    }
}

