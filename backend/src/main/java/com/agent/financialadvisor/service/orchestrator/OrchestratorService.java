package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    
    private final ChatLanguageModel chatLanguageModel;
    private final UserProfileAgent userProfileAgent;
    private final MarketAnalysisAgent marketAnalysisAgent;
    private final RiskAssessmentAgent riskAssessmentAgent;
    private final ResearchAgent researchAgent;
    private final RecommendationAgent recommendationAgent;
    private final WebSocketService webSocketService;
    
    // Cache for AI service instances per session
    private final Map<String, FinancialAdvisorAgent> agentCache = new HashMap<>();

    public OrchestratorService(
            ChatLanguageModel chatLanguageModel,
            UserProfileAgent userProfileAgent,
            MarketAnalysisAgent marketAnalysisAgent,
            RiskAssessmentAgent riskAssessmentAgent,
            ResearchAgent researchAgent,
            RecommendationAgent recommendationAgent,
            WebSocketService webSocketService
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.userProfileAgent = userProfileAgent;
        this.marketAnalysisAgent = marketAnalysisAgent;
        this.riskAssessmentAgent = riskAssessmentAgent;
        this.researchAgent = researchAgent;
        this.recommendationAgent = recommendationAgent;
        this.webSocketService = webSocketService;
    }

    /**
     * Main orchestration method - coordinates all agents to provide financial advice
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

            // Execute the agent with the query
            String response = agent.chat(sessionId, contextualQuery);

            // Send final response
            webSocketService.sendFinalResponse(sessionId, response);

            return response;
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
            return AiServices.builder(FinancialAdvisorAgent.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                    .tools(
                            userProfileAgent,
                            marketAnalysisAgent,
                            riskAssessmentAgent,
                            researchAgent,
                            recommendationAgent
                    )
                    .build();
        });
    }

    /**
     * Build a contextual query that includes user profile information
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

        contextualQuery.append("User Query: ").append(userQuery);
        contextualQuery.append("\n\nPlease analyze this request and use the available tools to provide a comprehensive financial recommendation. ");
        contextualQuery.append("If the user is asking about a stock, analyze it thoroughly using market analysis, risk assessment, and research tools. ");
        contextualQuery.append("Then synthesize all information into a clear recommendation.");

        return contextualQuery.toString();
    }

    /**
     * AI Agent interface - defines the conversation interface
     */
    public interface FinancialAdvisorAgent {
        @SystemMessage("You are a professional financial advisor AI assistant. " +
                "You coordinate multiple specialized agents to provide comprehensive investment advice. " +
                "You have access to tools from: User Profile Agent, Market Analysis Agent, Risk Assessment Agent, Research Agent, and Recommendation Agent. " +
                "Always consider the user's risk tolerance and investment goals when making recommendations. " +
                "Provide clear, well-reasoned recommendations based on data from all agents. " +
                "If a user profile doesn't exist, guide them to create one first. " +
                "Always include appropriate disclaimers about investment risks.")
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

