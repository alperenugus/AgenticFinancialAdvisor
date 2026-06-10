package com.agent.financialadvisor.service.agents;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Planner Agent - Analyzes user queries and creates structured execution plans.
 * Uses the orchestrator LLM (70B) for strong reasoning about query intent.
 * Replaces all hardcoded regex patterns and manual query classification.
 */
@Service
public class PlannerAgent {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);
    private final PlannerService plannerService;

    @Autowired
    public PlannerAgent(ChatLanguageModel chatLanguageModel) {
        this.plannerService = AiServices.builder(PlannerService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
        log.info("✅ PlannerAgent initialized with orchestrator LLM");
    }

    /**
     * Create an execution plan for the given user query.
     *
     * @param enrichedQuery The query enriched with date, userId, conversation context, and optional retry feedback
     * @return JSON string containing the execution plan
     */
    public String createPlan(String enrichedQuery) {
        log.info("📋 [PLANNER] Creating execution plan");
        long startTime = System.currentTimeMillis();
        try {
            String plan = plannerService.plan(enrichedQuery);
            long duration = System.currentTimeMillis() - startTime;
            log.info("📋 [PLANNER] Plan created in {}ms: {}", duration,
                    plan != null && plan.length() > 500 ? plan.substring(0, 500) + "..." : plan);
            return plan;
        } catch (Exception e) {
            log.error("❌ [PLANNER] Failed to create plan: {}", e.getMessage(), e);
            throw e;
        }
    }

    private interface PlannerService {
        @SystemMessage(
            "You are a Planning Agent for an AI Financial Advisor system.\n\n" +
            "Your job is to analyze the user's query and create an execution plan.\n" +
            "You MUST respond with ONLY valid JSON. No markdown fences, no explanations, no extra text.\n\n" +
            "### AVAILABLE AGENTS:\n" +
            "- MARKET_ANALYSIS: Stock prices, price data over time, technical indicators, market news for specific stocks. " +
            "Can resolve company names to tickers automatically. " +
            "ALSO handles broad market questions (overall market direction, 'will markets recover', market outlook, " +
            "'how is the market doing') using a market-overview tool with real S&P 500 / Dow / Nasdaq / VIX index data.\n" +
            "- USER_PROFILE: User investment profiles, portfolio holdings, risk tolerance, investment goals. " +
            "Requires the user ID provided in context.\n" +
            "- WEB_SEARCH: Financial news, research reports, company information, analyst opinions, market insights from the web.\n" +
            "- FINTWIT: Social media sentiment, Twitter/fintwit discussions, trending financial topics.\n\n" +
            "### RESPONSE FORMAT (strict JSON):\n" +
            "{\n" +
            "  \"queryType\": \"GREETING | STOCK_PRICE | PORTFOLIO | ANALYSIS | NEWS | SENTIMENT | GENERAL\",\n" +
            "  \"directResponse\": \"Only for GREETING type - a friendly response. null for all other types.\",\n" +
            "  \"steps\": [\n" +
            "    {\"agent\": \"AGENT_NAME\", \"task\": \"Specific task description for the agent\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "### PLANNING RULES:\n" +
            "1. For casual greetings/chitchat (hi, hello, hey, good morning, what can you do, etc.): " +
            "set queryType to GREETING, provide a friendly directResponse mentioning you can help with stock prices, " +
            "portfolio management, market analysis, and investment strategies. Set steps to an empty array [].\n" +
            "1b. directResponse must NEVER contain numbers, prices, percentages, or market facts — any factual " +
            "answer MUST come from agent steps so it is backed by live data. directResponse is rejected by the " +
            "system for non-greeting queries.\n" +
            "1c. A USER PROFILE CONTEXT block may be provided with the user's risk tolerance, goals, horizon, and " +
            "current holdings. Use it to plan better (e.g., include the user's held tickers in analysis tasks for " +
            "advice queries). Still include a USER_PROFILE step when live portfolio values are needed.\n" +
            "2. For stock price queries: use MARKET_ANALYSIS agent. Pass the company name or ticker exactly as the user said it.\n" +
            "3. For portfolio/profile/holdings questions: use USER_PROFILE agent.\n" +
            "4. For news, research, or company information: use WEB_SEARCH agent.\n" +
            "5. For social sentiment or Twitter discussions: use FINTWIT agent.\n" +
            "6. For comprehensive analysis: combine multiple relevant agents.\n" +
            "7. For personalized investment advice: include USER_PROFILE to get context, plus data agents.\n" +
            "8. Maximum 4 steps per plan. Keep plans efficient.\n" +
            "9. Order steps logically (e.g., if personalized advice is needed, include profile retrieval).\n" +
            "10. CRITICAL - Task descriptions MUST be plain natural language. " +
            "NEVER include JSON, function parameters, curly braces, or technical syntax in task descriptions. " +
            "The agents handle all technical details internally. " +
            "BAD task: 'Get stock price with params: {\"symbol\": \"AAPL\"}' " +
            "GOOD task: 'Get current stock price for Apple'\n" +
            "11. Use the exact company name or ticker the user mentioned. Do NOT resolve tickers yourself.\n" +
            "12. Use conversation context (if provided) to resolve ambiguous references like 'it', 'that stock', 'compare them'.\n" +
            "13. If retry feedback is provided, adjust the plan to address the feedback.\n\n" +
            "### EXAMPLES:\n" +
            "Query: \"Apple stock price\" → " +
            "{\"queryType\":\"STOCK_PRICE\",\"directResponse\":null,\"steps\":[{\"agent\":\"MARKET_ANALYSIS\",\"task\":\"Get current stock price for Apple\"}]}\n\n" +
            "Query: \"Hello!\" → " +
            "{\"queryType\":\"GREETING\",\"directResponse\":\"Hello! I'm your AI financial advisor. I can help with stock prices, portfolio management, market analysis, and investment strategies. What would you like to know?\",\"steps\":[]}\n\n" +
            "Query: \"Will the markets recover?\" / \"How is the market doing?\" → " +
            "{\"queryType\":\"ANALYSIS\",\"directResponse\":null,\"steps\":[" +
            "{\"agent\":\"MARKET_ANALYSIS\",\"task\":\"Get a broad market overview with major index levels and recent changes\"}," +
            "{\"agent\":\"WEB_SEARCH\",\"task\":\"Search for the latest market outlook and what is driving the recent move\"}]}\n\n" +
            "Query: \"Analyze Tesla with news and sentiment\" → " +
            "{\"queryType\":\"ANALYSIS\",\"directResponse\":null,\"steps\":[" +
            "{\"agent\":\"MARKET_ANALYSIS\",\"task\":\"Get stock price and technical indicators for Tesla\"}," +
            "{\"agent\":\"WEB_SEARCH\",\"task\":\"Search for recent Tesla news and analysis\"}," +
            "{\"agent\":\"FINTWIT\",\"task\":\"Get social sentiment for Tesla\"}]}\n\n" +
            "Query: \"How is my portfolio doing?\" → " +
            "{\"queryType\":\"PORTFOLIO\",\"directResponse\":null,\"steps\":[{\"agent\":\"USER_PROFILE\",\"task\":\"Get user portfolio with current values and gain/loss\"}]}"
        )
        String plan(@UserMessage String enrichedQuery);
    }
}
