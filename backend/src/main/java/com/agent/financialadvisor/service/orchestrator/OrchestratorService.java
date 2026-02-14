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
            // IMPORTANT: Set sessionId BEFORE creating the CompletableFuture so it's inherited
            ToolCallAspect.setSessionId(sessionId);
            log.info("🔧 Set sessionId={} in ThreadLocal for tool tracking", sessionId);
            
            CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
                // SessionId is inherited via InheritableThreadLocal
                String currentSessionId = ToolCallAspect.getSessionId();
                log.info("🔧 Thread {} has sessionId={} for tool tracking", Thread.currentThread().getName(), currentSessionId);
                
                try {
                    // Send reasoning step before agent execution
                    webSocketService.sendReasoning(sessionId, "🤔 Analyzing your request and determining which tools to use...");
                    
                    return agent.chat(sessionId, contextualQuery);
                } catch (Exception e) {
                    log.error("Error in agent chat execution: {}", e.getMessage(), e);
                    webSocketService.sendReasoning(sessionId, "❌ Error during analysis: " + e.getMessage());
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
            } finally {
                // Always clear session ID after execution
                ToolCallAspect.clearSessionId();
                log.info("🔧 Cleared sessionId from ThreadLocal");
            }
        } catch (Exception e) {
            log.error("Error in orchestration: {}", e.getMessage(), e);
            String errorMsg = "I apologize, but I encountered an error while processing your request. Please try again.";
            webSocketService.sendError(sessionId, errorMsg);
            ToolCallAspect.clearSessionId(); // Clear on error too
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
        contextualQuery.append("If the user is asking about a stock, analyze it thoroughly using market analysis tools, web search for latest news, and fintwit for social sentiment. ");
        contextualQuery.append("Consider how recommendations fit with their current portfolio and risk tolerance. ");
        contextualQuery.append("Then synthesize all information into a clear recommendation.");

        return contextualQuery.toString();
    }

    /**
     * AI Agent interface - defines the conversation interface
     */
           public interface FinancialAdvisorAgent {
               @SystemMessage("You are a professional financial analyst providing personalized investment advice. " +
                       "You have access to 4 specialized tools:\n" +
                       "1. Portfolio Management - Access user's portfolio, profile, and investment preferences\n" +
                       "2. Market Analysis - Get real-time stock prices, price data, and technical indicators\n" +
                       "3. Web Search - Search for latest financial news, analysis, and market insights\n" +
                       "4. Fintwit Analysis - Analyze social sentiment from financial Twitter\n" +
                       "\n" +
                       "REASONING PROCESS:\n" +
                       "1. When a user asks a question, first think about what information you need\n" +
                       "2. Use the appropriate tools to gather that information (the system will show your tool usage)\n" +
                       "3. Analyze the results from the tools\n" +
                       "4. Synthesize your findings into a clear, professional response\n" +
                       "\n" +
                       "CRITICAL RULES:\n" +
                       "- ALWAYS use tools to get the most up-to-date data. NEVER use prices or information from your training data.\n" +
                       "- When analyzing a user's portfolio, you MUST call getPortfolio(userId) to get complete portfolio details.\n" +
                       "- Stock prices change constantly - always use tools before mentioning any price.\n" +
                       "- For comprehensive analysis, combine market data, web search results, and social sentiment.\n" +
                       "\n" +
                       "RESPONSE STYLE:\n" +
                       "- For analysis requests, provide professional insights including technical patterns, stop-loss levels, entry/exit prices.\n" +
                       "- Address the user directly using 'you' and 'your' (not 'the user' or 'user's').\n" +
                       "- When users greet you, respond warmly and guide them to share their financial questions.")
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

