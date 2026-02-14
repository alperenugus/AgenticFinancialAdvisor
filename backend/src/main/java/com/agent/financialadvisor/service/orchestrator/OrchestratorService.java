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
    private final WebSearchAgent webSearchAgent;
    private final FintwitAnalysisAgent fintwitAnalysisAgent;
    private final WebSocketService webSocketService;
    private final int orchestratorTimeoutSeconds;
    
    // Cache for AI service instances per session
    private final Map<String, FinancialAdvisorAgent> agentCache = new HashMap<>();
    
    // Store plans per session waiting for user confirmation

    public OrchestratorService(
            ChatLanguageModel chatLanguageModel,
            UserProfileAgent userProfileAgent,
            MarketAnalysisAgent marketAnalysisAgent,
            WebSearchAgent webSearchAgent,
            FintwitAnalysisAgent fintwitAnalysisAgent,
            WebSocketService webSocketService,
            @Value("${agent.timeout.orchestrator-seconds:60}") int orchestratorTimeoutSeconds
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.userProfileAgent = userProfileAgent;
        this.marketAnalysisAgent = marketAnalysisAgent;
        this.webSearchAgent = webSearchAgent;
        this.fintwitAnalysisAgent = fintwitAnalysisAgent;
        this.webSocketService = webSocketService;
        this.orchestratorTimeoutSeconds = orchestratorTimeoutSeconds;
        
        log.info("✅ Orchestrator initialized with 4 agents: UserProfile, MarketAnalysis, WebSearch, Fintwit");
    }

    /**
     * Main orchestration method - coordinates all agents to provide financial advice
     */
    public String coordinateAnalysis(String userId, String userQuery, String sessionId) {
        log.info("🎯 Orchestrator coordinating analysis for userId={}, query={}", userId, userQuery);
        
        try {
            // Check if this is a casual greeting - if so, don't call tools
            if (isCasualGreeting(userQuery)) {
                log.info("📝 Detected casual greeting, responding without tools");
                String greetingResponse = "Hello! I'm your AI financial advisor. I can help you with stock analysis, portfolio management, investment strategies, and market insights. What would you like to know?";
                webSocketService.sendFinalResponse(sessionId, greetingResponse);
                return greetingResponse;
            }

            // Execute directly - no planning phase
            return executeQuery(userId, userQuery, sessionId);
        } catch (Exception e) {
            log.error("Error in orchestration: {}", e.getMessage(), e);
            String errorMsg = "I apologize, but I encountered an error while processing your request. Please try again.";
            webSocketService.sendError(sessionId, errorMsg);
            return errorMsg;
        }
    }

    /**
     * Execute user query directly
     */
    private String executeQuery(String userId, String userQuery, String sessionId) {
        try {
            log.info("🔧 Executing query for sessionId={}, query={}", sessionId, userQuery);
            
            // Get or create AI agent for this session
            FinancialAdvisorAgent agent = getOrCreateAgent(sessionId);
            
            // Build contextual query
            String contextualQuery = buildContextualQuery(userId, userQuery, sessionId);
            
            // Execute the agent with timeout protection
            ToolCallAspect.setSessionId(sessionId);
            
            CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("🚀 Starting agent chat execution for sessionId={}", sessionId);
                    String result = agent.chat(sessionId, contextualQuery);
                    log.info("✅ Agent chat execution completed for sessionId={}, response length={}", sessionId, result != null ? result.length() : 0);
                    return result;
                } catch (Exception e) {
                    log.error("❌ Error in query execution: {}", e.getMessage(), e);
                    throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
                }
            });

            String response;
            try {
                log.info("⏳ Waiting for response (timeout: {}s) for sessionId={}", orchestratorTimeoutSeconds, sessionId);
                response = futureResponse.get(orchestratorTimeoutSeconds, TimeUnit.SECONDS);
                log.info("✅ Got response for sessionId={}, length={}", sessionId, response != null ? response.length() : 0);
                
                if (response == null || response.trim().isEmpty()) {
                    log.warn("⚠️ Empty response received for sessionId={}", sessionId);
                    response = "I apologize, but I didn't receive a valid response. Please try again.";
                }
                
                webSocketService.sendFinalResponse(sessionId, response);
                return response;
            } catch (TimeoutException e) {
                log.warn("⏱️ Query execution timeout after {} seconds for sessionId={}", 
                        orchestratorTimeoutSeconds, sessionId);
                futureResponse.cancel(true);
                String timeoutMsg = String.format(
                    "I apologize, but the analysis took longer than expected (exceeded %d seconds). " +
                    "Please try again with a simpler query.",
                    orchestratorTimeoutSeconds
                );
                webSocketService.sendError(sessionId, timeoutMsg);
                return timeoutMsg;
            } finally {
                ToolCallAspect.clearSessionId();
                log.info("🧹 Cleared session context for sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("❌ Error executing query: {}", e.getMessage(), e);
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
                            userProfileAgent,      // Portfolio & user profile management
                            marketAnalysisAgent,   // Real-time market data & price analysis
                            webSearchAgent,        // Web search for financial news & analysis
                            fintwitAnalysisAgent   // Social sentiment analysis from fintwit
                    )
                    .build();
        });
    }

    /**
     * Check if the query is a casual greeting
     */
    private boolean isCasualGreeting(String query) {
        String lowerQuery = query.toLowerCase().trim();
        String[] greetings = {"hello", "hi", "hey", "good morning", "good afternoon", "good evening", 
                             "how are you", "what's up", "what can you do", "what do you do"};
        for (String greeting : greetings) {
            if (lowerQuery.equals(greeting) || lowerQuery.startsWith(greeting + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build a contextual query that includes user profile and portfolio information
     * This is called by the orchestrator to gather context before delegating to agents
     */
    private String buildContextualQuery(String userId, String userQuery, String sessionId) {
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
        contextualQuery.append("\n\n");
        // Check if this is a stock price query and add explicit tool requirement
        String lowerQuery = userQuery.toLowerCase();
        boolean isPriceQuery = lowerQuery.contains("price") || lowerQuery.contains("stock price") || 
                              lowerQuery.matches(".*\\b(aapl|msft|googl|amzn|nvda|tsla|meta|nflx|dis|v|ma|jpm|bac|wmt|pg|ko|pep|mcd|sbux|nke|adbe|orcl|intc|amd|qcom|avgo|txn|mu|amd|intel|microsoft|apple|google|amazon|nvidia|tesla|meta|netflix|disney|visa|mastercard|jpmorgan|bank of america|walmart|procter|gamble|coca-cola|pepsi|mcdonalds|starbucks|nike|adobe|oracle|qualcomm|broadcom|texas instruments|micron|advanced micro devices)\\b.*");
        
        if (isPriceQuery) {
            contextualQuery.append("### ⚠️ CRITICAL: STOCK PRICE QUERY DETECTED ⚠️\n");
            contextualQuery.append("**YOU ARE ASKED ABOUT A STOCK PRICE. THIS IS MANDATORY:**\n");
            contextualQuery.append("1. You MUST call getStockPrice(symbol) tool - this is NOT optional\n");
            contextualQuery.append("2. You CANNOT use training data - your training data is OUTDATED\n");
            contextualQuery.append("3. You CANNOT guess or estimate - you MUST call the tool\n");
            contextualQuery.append("4. Extract the symbol from the query (e.g., 'apple' or 'AAPL' → 'AAPL')\n");
            contextualQuery.append("5. Call getStockPrice with the correct symbol\n");
            contextualQuery.append("6. Use ONLY the price returned by the tool in your response\n");
            contextualQuery.append("**IF YOU DO NOT CALL getStockPrice, YOUR RESPONSE IS WRONG**\n\n");
        }
        
        contextualQuery.append("### CRITICAL: DO NOT EXPLAIN YOUR PROCESS\n");
        contextualQuery.append("**ABSOLUTELY FORBIDDEN**:\n");
        contextualQuery.append("- NEVER explain what you're going to do (e.g., \"I first need to consider\", \"I will utilize\", \"Given this\")\n");
        contextualQuery.append("- NEVER describe your reasoning process to the user\n");
        contextualQuery.append("- NEVER say things like \"To answer your question, I first need to...\"\n");
        contextualQuery.append("- NEVER explain which tools you're using or why\n");
        contextualQuery.append("- Your thinking process is INTERNAL ONLY - it's shown in the Agent Thinking panel, not in your response\n\n");
        contextualQuery.append("**WHAT TO DO INSTEAD**:\n");
        contextualQuery.append("- Just directly answer the question\n");
        contextualQuery.append("- Use tools silently (they're called automatically)\n");
        contextualQuery.append("- Provide the answer as if you already know it\n");
        contextualQuery.append("- Be concise and direct\n\n");
        contextualQuery.append("### IMPORTANT:\n");
        contextualQuery.append("- You have access to the user's profile and portfolio data above\n");
        contextualQuery.append("- Always use tools for current data - never use training data\n");
        contextualQuery.append("- Address the user directly using 'you' and 'your'\n");
        contextualQuery.append("- For simple questions (like price queries), give a direct answer without explanation\n");

        return contextualQuery.toString();
    }

    /**
     * AI Agent interface - defines the conversation interface
     */
           public interface FinancialAdvisorAgent {
               @SystemMessage("You are a Financial Advisor Manager. You coordinate with specialized agents to handle user requests.\n\n" +
                       "### YOUR ROLE:\n" +
                       "You are a manager of very capable agents. Your job is to handle user requests by using the appropriate agents and their tools. You can use multiple agents if needed.\n\n" +
                       "### YOUR AGENTS:\n" +
                       "- **UserProfileAgent**: Portfolio and user profile data\n" +
                       "- **MarketAnalysisAgent**: Real-time stock prices and market data\n" +
                       "- **WebSearchAgent**: Financial news and analysis search\n" +
                       "- **FintwitAnalysisAgent**: Social sentiment analysis from financial Twitter\n\n" +
                       "### HOW TO WORK:\n" +
                       "- Handle user requests directly by using the appropriate agents and tools\n" +
                       "- For simple queries (e.g., stock prices), use the MarketAnalysisAgent\n" +
                       "- For complex queries, you can use multiple agents to gather comprehensive information\n" +
                       "- Always use tools for current data - never use training data or guess\n" +
                       "- For stock prices, ALWAYS use getStockPrice - your training data is outdated\n" +
                       "- Be direct and conversational - answer as if you already know the information\n" +
                       "- Never explain your process or reasoning - just provide the answer\n" +
                       "- Address users with \"you\" and \"your\"\n\n" +
                       "### AVAILABLE TOOLS:\n" +
                       "**Portfolio Tools:** getUserProfile, getPortfolio, getPortfolioHoldings, getPortfolioSummary, getInvestmentGoals, updateRiskTolerance\n" +
                       "**Market Tools:** getStockPrice, getStockPriceData, getMarketNews, analyzeTrends, getTechnicalIndicators\n" +
                       "**Web Search Tools:** searchFinancialNews, searchStockAnalysis, searchMarketTrends, searchCompanyInfo\n" +
                       "**Fintwit Tools:** getFintwitSentiment, getFintwitTrends, analyzeFintwitSentiment\n\n" +
                       "### RULES:\n" +
                       "- Always use tools for current data - never use training data or guess\n" +
                       "- For stock prices, ALWAYS use getStockPrice - your training data is outdated\n" +
                       "- Be direct - answer as if you already know, don't explain your process\n" +
                       "- Never show tool calls or function syntax in responses\n" +
                       "- For greetings, respond directly without tools")
               String chat(@MemoryId String sessionId, @UserMessage String userMessage);
           }

    /**
     * Check agent status - verify all agents are available
     */
    public Map<String, Boolean> getAgentStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("userProfileAgent", userProfileAgent != null);
        status.put("marketAnalysisAgent", marketAnalysisAgent != null);
        status.put("webSearchAgent", webSearchAgent != null);
        status.put("fintwitAnalysisAgent", fintwitAnalysisAgent != null);
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

