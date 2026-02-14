package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.*;
import com.agent.financialadvisor.aspect.ToolCallAspect;
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
           private final WebSearchAgent webSearchAgent; // NEW: Web search capabilities
           private final FintwitAnalysisAgent fintwitAnalysisAgent; // NEW: Fintwit analysis
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
                   WebSearchAgent webSearchAgent, // NEW: Web search agent
                   FintwitAnalysisAgent fintwitAnalysisAgent, // NEW: Fintwit analysis agent
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
               this.webSearchAgent = webSearchAgent; // Assign web search agent
               this.fintwitAnalysisAgent = fintwitAnalysisAgent; // Assign fintwit agent
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
                // Set session ID in ThreadLocal for tool tracking (used by agents)
                ToolCallAspect.setSessionId(sessionId);
                try {
                    return agent.chat(sessionId, contextualQuery);
                } catch (Exception e) {
                    log.error("Error in agent chat execution: {}", e.getMessage(), e);
                    throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
                } finally {
                    // Clear session ID after execution
                    ToolCallAspect.clearSessionId();
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
            
            // Note: Tool tracking is done directly in tool methods via WebSocketService
            // Session ID is set in ThreadLocal and accessed by agents via ToolCallAspect.getSessionId()
            
            return AiServices.builder(FinancialAdvisorAgent.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemoryProvider(memoryProvider)
                    .tools(
                            userProfileAgent,
                            marketAnalysisAgent,
                            riskAssessmentAgent,
                            researchAgent,
                            recommendationAgent,
                            stockDiscoveryAgent,
                            webSearchAgent, // NEW: Web search tools
                            fintwitAnalysisAgent // NEW: Fintwit analysis tools
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
                       "You have access to tools from: User Profile Agent (portfolio & profile access), Market Analysis Agent (real-time market data), " +
                       "Risk Assessment Agent, Research Agent (fundamentals), Stock Discovery Agent, Recommendation Agent, " +
                       "Web Search Agent (latest financial news & analysis), and Fintwit Analysis Agent (social sentiment). " +
                       "CRITICAL - TOOL CALLING IS MANDATORY: " +
                       "You MUST explicitly think about calling tools when you need data. LangChain4j will detect your intent and call the tools. " +
                       "When you need stock prices, you MUST think: 'I need to call getStockPrice for each symbol' " +
                       "When you need portfolio data, you MUST think: 'I need to call getPortfolio' " +
                       "When you need technical analysis, you MUST think: 'I need to call analyzeTrends and getTechnicalIndicators' " +
                       "The tools are NOT called automatically - you must think about needing them explicitly. " +
                       "DO NOT use placeholders like $X, $Y, [Current Price], etc. - these are FORBIDDEN. " +
                       "DO NOT say 'I see that the current prices are...' without actually calling getStockPrice first. " +
                       "DO NOT make up prices or use training data. " +
                       "If you mention a price, you MUST have called getStockPrice(symbol) first and use the EXACT price returned. " +
                "CRITICAL COMMUNICATION STYLE: When responding to the user, ALWAYS address them DIRECTLY using 'you', 'your', 'yours'. " +
                "NEVER refer to 'the user', 'user's', 'the user's portfolio', etc. " +
                "Examples: " +
                "- CORRECT: 'Based on your portfolio and risk tolerance...' " +
                "- WRONG: 'Based on the user's portfolio and risk tolerance...' " +
                "- CORRECT: 'Your portfolio shows...' " +
                "- WRONG: 'The user's portfolio shows...' " +
                "- CORRECT: 'I recommend you consider...' " +
                "- WRONG: 'I recommend the user consider...' " +
                "This makes the conversation natural, personal, and professional. " +
                "Always check the user's current portfolio before making recommendations. " +
                "Always consider the user's risk tolerance, investment goals, and current portfolio when making recommendations. " +
                "When users greet you (hello, hi, hey, etc.), respond warmly and briefly introduce yourself as their financial advisor. " +
                "Then guide them to share their financial questions or goals. Do NOT immediately provide stock recommendations for greetings. " +
                "For greetings, say something like: 'Hello! I'm your AI financial advisor. I can help you with investment advice, portfolio analysis, and personalized recommendations. " +
                "What would you like to know about your investments today?' " +
                "Only provide investment recommendations when the user asks specific questions about stocks, their portfolio, or investment advice. " +
                "ABSOLUTELY CRITICAL - DATA FRESHNESS RULES: " +
                "1. You MUST ALWAYS call tools to get current stock prices. NEVER use prices from your training data or memory. " +
                "2. Stock prices change constantly - prices from even minutes ago are outdated. " +
                "3. If you mention ANY stock price, you MUST have called getStockPrice(symbol) FIRST to get the current price. " +
                "4. If you mention technical indicators, trends, or patterns, you MUST have called getStockPriceData(symbol, timeframe) FIRST. " +
                "5. NEVER guess prices, NEVER use training data prices, NEVER assume prices. " +
                "6. Example: If analyzing NVDA, you MUST call getStockPrice('NVDA') first, then use that exact price in your analysis. " +
                "7. If you don't call the tool first, you are using outdated training data which is WRONG and UNACCEPTABLE. " +
                "8. All market data tools fetch FRESH, REAL-TIME data from external APIs. There is NO caching. " +
                "9. Every tool call makes a fresh API request to get the latest available data. " +
                "10. Note: Free tier data may have a 15-minute delay during market hours. Premium tier provides real-time data. " +
                "CRITICAL: When providing recommendations, you MUST provide PROFESSIONAL FINANCIAL ANALYST-level analysis including: " +
                "1. Technical analysis patterns (head and shoulders, double tops/bottoms, triangles, support/resistance levels, etc.) " +
                "2. Stop loss levels (specific price points where to set stop losses, e.g., 'For this stock, you can have a stop loss at $X') " +
                "3. Averaging down advice (if applicable, e.g., 'For this stock, you can average down a bit if it drops to $X') " +
                "4. Entry and exit price suggestions " +
                "5. Target price with reasoning " +
                "6. Professional analysis of chart patterns, trends, and technical indicators " +
                "7. Portfolio management advice considering current holdings " +
                "MANDATORY WORKFLOW FOR ANY STOCK ANALYSIS - YOU MUST FOLLOW THIS EXACTLY: " +
                "Step 1: Think 'I need to get the current price for [SYMBOL]' - this will trigger getStockPrice(symbol) " +
                "Step 2: Wait for the tool result, then use the EXACT price returned (e.g., if tool returns $150.25, use $150.25, NOT $150 or $X) " +
                "Step 3: Think 'I need price history for [SYMBOL]' - this will trigger getStockPriceData(symbol, 'daily') " +
                "Step 4: Think 'I need technical analysis for [SYMBOL]' - this will trigger analyzeTrends(symbol, 'daily') " +
                "Step 5: Think 'I need technical indicators for [SYMBOL]' - this will trigger getTechnicalIndicators(symbol) " +
                "Step 6: Use the ACTUAL data from tool results in your response - quote the exact prices, exact numbers " +
                "NEVER skip these steps. NEVER use placeholders. NEVER use training data. NEVER guess. " +
                "If you write '$X' or '[Current Price]' or any placeholder, you have FAILED. " +
                "You MUST use the exact values returned by the tools. " +
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
        status.put("stockDiscoveryAgent", stockDiscoveryAgent != null);
        status.put("webSearchAgent", webSearchAgent != null); // NEW: Web search agent status
        status.put("fintwitAnalysisAgent", fintwitAnalysisAgent != null); // NEW: Fintwit agent status
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

