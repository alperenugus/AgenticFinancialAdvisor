package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.*;
import com.agent.financialadvisor.aspect.ToolCallAspect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrator Service - Coordinates the Plan-Execute-Evaluate agentic loop.
 *
 * Architecture:
 *   1. Security validation (deterministic + LLM hybrid)
 *   2. PlannerAgent creates a structured execution plan from the user query
 *   3. Executor runs plan steps in parallel by delegating to sub-agents
 *   4. EvaluatorAgent reviews results and synthesizes the final response
 *   5. If evaluator requests retry, loop back to step 2 (max 2 retries)
 *
 * All query understanding is done by LLMs. No regex patterns, no hardcoded
 * greeting lists, no manual stock-price bypasses.
 */
@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    private static final int MAX_PLAN_RETRIES = 2;
    private static final int MAX_CONVERSATION_HISTORY = 5;
    private static final int MAX_PLAN_STEPS = 4;

    private final PlannerAgent plannerAgent;
    private final EvaluatorAgent evaluatorAgent;
    private final UserProfileAgent userProfileAgent;
    private final MarketAnalysisAgent marketAnalysisAgent;
    private final WebSearchAgent webSearchAgent;
    private final FintwitAnalysisAgent fintwitAnalysisAgent;
    private final SecurityAgent securityAgent;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final int orchestratorTimeoutSeconds;
    private final int toolCallTimeoutSeconds;
    private final ExecutorService agentExecutor;

    private final Map<String, String> sessionUserIdCache = new ConcurrentHashMap<>();
    private final Map<String, List<ConversationTurn>> conversationHistory = new ConcurrentHashMap<>();

    private record ConversationTurn(String userQuery, String assistantResponse) {}

    public OrchestratorService(
            PlannerAgent plannerAgent,
            EvaluatorAgent evaluatorAgent,
            UserProfileAgent userProfileAgent,
            MarketAnalysisAgent marketAnalysisAgent,
            WebSearchAgent webSearchAgent,
            FintwitAnalysisAgent fintwitAnalysisAgent,
            SecurityAgent securityAgent,
            WebSocketService webSocketService,
            ObjectMapper objectMapper,
            @Value("${agent.timeout.orchestrator-seconds:90}") int orchestratorTimeoutSeconds,
            @Value("${agent.timeout.tool-call-seconds:10}") int toolCallTimeoutSeconds
    ) {
        this.plannerAgent = plannerAgent;
        this.evaluatorAgent = evaluatorAgent;
        this.userProfileAgent = userProfileAgent;
        this.marketAnalysisAgent = marketAnalysisAgent;
        this.webSearchAgent = webSearchAgent;
        this.fintwitAnalysisAgent = fintwitAnalysisAgent;
        this.securityAgent = securityAgent;
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
        this.orchestratorTimeoutSeconds = orchestratorTimeoutSeconds;
        this.toolCallTimeoutSeconds = toolCallTimeoutSeconds;
        this.agentExecutor = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors())
        );

        log.info("✅ Orchestrator initialized with Plan-Execute-Evaluate architecture: " +
                "PlannerAgent, EvaluatorAgent, UserProfile, MarketAnalysis, WebSearch, Fintwit, Security");
    }

    /**
     * Main entry point - coordinates the Plan-Execute-Evaluate loop for a user query.
     */
    public String coordinateAnalysis(String userId, String userQuery, String sessionId) {
        log.info("🎯 Orchestrator coordinating analysis for userId={}, query={}", userId, userQuery);
        sessionUserIdCache.put(sessionId, userId);

        try {
            // Step 1: Security validation
            SecurityAgent.SecurityValidationResult validation = securityAgent.validateInput(userQuery);
            if (!validation.isSafe()) {
                log.warn("🚫 Unsafe input detected: {}", validation.getReason());
                String errorMsg = "I apologize, but I cannot process that request. " +
                        "Please rephrase your question to focus on financial advisory services. " +
                        "I can help you with stock analysis, portfolio management, and investment strategies.";
                webSocketService.sendError(sessionId, errorMsg);
                return errorMsg;
            }

            // Step 2: Plan-Execute-Evaluate loop (with overall timeout)
            return executePlanLoop(userId, userQuery, sessionId);
        } catch (Exception e) {
            log.error("Error in orchestration: {}", e.getMessage(), e);
            String errorMsg = "I apologize, but I encountered an error while processing your request. Please try again.";
            webSocketService.sendError(sessionId, errorMsg);
            return errorMsg;
        }
    }

    private String executePlanLoop(String userId, String userQuery, String sessionId) {
        CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
            ToolCallAspect.setSessionId(sessionId);
            try {
                return runPlanExecuteEvaluate(userId, userQuery, sessionId);
            } finally {
                ToolCallAspect.clearSessionId();
            }
        });

        try {
            log.info("⏳ Waiting for plan-execute-evaluate loop (timeout: {}s) for sessionId={}",
                    orchestratorTimeoutSeconds, sessionId);
            String response = futureResponse.get(orchestratorTimeoutSeconds, TimeUnit.SECONDS);

            if (response == null || response.trim().isEmpty()) {
                response = "I apologize, but I didn't receive a valid response. Please try again.";
            }

            addConversationTurn(sessionId, userQuery, response);
            webSocketService.sendFinalResponse(sessionId, response);
            return response;
        } catch (TimeoutException e) {
            log.warn("⏱️ Plan-execute-evaluate loop timed out after {}s for sessionId={}",
                    orchestratorTimeoutSeconds, sessionId);
            futureResponse.cancel(true);
            String timeoutMsg = String.format(
                    "I apologize, but the analysis took longer than expected (exceeded %d seconds). " +
                    "Please try again with a simpler query.",
                    orchestratorTimeoutSeconds
            );
            webSocketService.sendError(sessionId, timeoutMsg);
            return timeoutMsg;
        } catch (Exception e) {
            log.error("❌ Error in plan-execute-evaluate loop: {}", e.getMessage(), e);
            String errorMsg = "I apologize, but I encountered an error while processing your request. Please try again.";
            webSocketService.sendError(sessionId, errorMsg);
            return errorMsg;
        }
    }

    /**
     * Core Plan-Execute-Evaluate loop with retry support.
     */
    private String runPlanExecuteEvaluate(String userId, String userQuery, String sessionId) {
        String lastFeedback = null;
        Map<String, String> lastResults = null;

        for (int attempt = 0; attempt <= MAX_PLAN_RETRIES; attempt++) {
            if (attempt > 0) {
                log.info("🔄 Retry attempt {} for sessionId={}", attempt, sessionId);
                webSocketService.sendReasoning(sessionId,
                        "🔄 Refining approach based on feedback (attempt " + (attempt + 1) + ")...");
            }

            // --- PLAN ---
            webSocketService.sendReasoning(sessionId, "📋 Analyzing your question and creating a plan...");
            String plannerInput = buildPlannerInput(userQuery, userId, sessionId, lastFeedback);
            log.info("📤 [PLAN] Planner input for attempt {}: {}", attempt,
                    plannerInput.length() > 500 ? plannerInput.substring(0, 500) + "..." : plannerInput);

            String planJson;
            try {
                planJson = plannerAgent.createPlan(plannerInput);
            } catch (Exception e) {
                log.error("❌ [PLAN] Planner failed on attempt {}: {}", attempt, e.getMessage(), e);
                if (attempt < MAX_PLAN_RETRIES) {
                    lastFeedback = "Planner failed: " + e.getMessage() + ". Try a simpler, more direct plan.";
                    continue;
                }
                return "I apologize, but I had trouble understanding your request. Please try rephrasing your question.";
            }

            log.info("📥 [PLAN] Raw planner response (attempt {}): {}", attempt, planJson);

            JsonNode plan = extractJson(planJson);
            if (plan == null) {
                log.warn("⚠️ [PLAN] Could not parse plan JSON on attempt {}: {}", attempt, planJson);
                if (attempt < MAX_PLAN_RETRIES) {
                    lastFeedback = "Previous plan was not valid JSON. Produce a simpler, valid JSON plan.";
                    continue;
                }
                return "I apologize, but I had trouble processing your request. Please try again.";
            }

            log.info("📋 [PLAN] Parsed plan (attempt {}): queryType={}, directResponse={}, steps={}",
                    attempt,
                    plan.path("queryType").asText("?"),
                    plan.path("directResponse").asText("null"),
                    plan.path("steps").size());

            // Check for direct response (greetings, chitchat)
            String directResponse = plan.path("directResponse").asText(null);
            if (directResponse != null && !directResponse.isEmpty() && !"null".equals(directResponse)) {
                log.info("📝 Direct response from planner: {}", directResponse);
                return directResponse;
            }

            // --- EXECUTE ---
            JsonNode stepsNode = plan.path("steps");
            if (!stepsNode.isArray() || stepsNode.isEmpty()) {
                log.warn("⚠️ [EXECUTE] Plan has no execution steps on attempt {}", attempt);
                return "I'm not sure how to help with that. Could you please ask a question about " +
                       "stocks, portfolios, or financial markets?";
            }

            int stepCount = Math.min(stepsNode.size(), MAX_PLAN_STEPS);
            for (int s = 0; s < stepCount; s++) {
                log.info("📋 [EXECUTE] Step {}: agent={}, task={}", s + 1,
                        stepsNode.get(s).path("agent").asText("?"),
                        stepsNode.get(s).path("task").asText("?"));
            }
            webSocketService.sendReasoning(sessionId,
                    "🔧 Executing plan with " + stepCount + " step(s)...");

            Map<String, String> results = executePlanSteps(stepsNode, userId, sessionId);
            lastResults = results;
            log.info("✅ [EXECUTE] Execution complete: {} results collected for sessionId={}", results.size(), sessionId);
            for (Map.Entry<String, String> entry : results.entrySet()) {
                String preview = entry.getValue();
                if (preview != null && preview.length() > 300) {
                    preview = preview.substring(0, 300) + "...";
                }
                log.info("📥 [EXECUTE] {}: {}", entry.getKey(), preview);
            }

            // --- EVALUATE ---
            webSocketService.sendReasoning(sessionId, "🔍 Analyzing results...");
            String evaluationInput = buildEvaluationInput(userQuery, planJson, results);
            String evaluationJson;
            try {
                evaluationJson = evaluatorAgent.evaluate(evaluationInput);
            } catch (Exception e) {
                log.error("❌ [EVALUATE] Evaluator failed: {}", e.getMessage(), e);
                return buildFallbackResponse(results);
            }

            log.info("📥 [EVALUATE] Raw evaluator response (attempt {}): {}", attempt,
                    evaluationJson != null && evaluationJson.length() > 500
                            ? evaluationJson.substring(0, 500) + "..." : evaluationJson);

            JsonNode evaluation = extractJson(evaluationJson);
            if (evaluation == null) {
                log.warn("⚠️ [EVALUATE] Could not parse evaluation JSON, using raw evaluator output");
                return evaluationJson != null ? evaluationJson.trim() : buildFallbackResponse(results);
            }

            String verdict = evaluation.path("verdict").asText("PASS");
            log.info("🔍 [EVALUATE] Verdict={} for attempt {} sessionId={}", verdict, attempt, sessionId);

            if ("PASS".equalsIgnoreCase(verdict)) {
                String response = evaluation.path("response").asText("");
                if (response.isEmpty()) {
                    log.warn("⚠️ [EVALUATE] PASS verdict but empty response, using fallback");
                    return buildFallbackResponse(results);
                }
                log.info("✅ [EVALUATE] PASSED - response length={}", response.length());
                return response;
            }

            // RETRY requested by evaluator
            lastFeedback = evaluation.path("feedback").asText("Results were insufficient. Try a different approach.");
            log.info("🔄 [EVALUATE] Evaluator requested RETRY (attempt {}): {}", attempt, lastFeedback);
        }

        log.warn("⚠️ All {} attempts exhausted for sessionId={}", MAX_PLAN_RETRIES + 1, sessionId);
        if (lastResults != null && !lastResults.isEmpty()) {
            return buildFallbackResponse(lastResults);
        }
        return "I apologize, but I wasn't able to fully answer your question after multiple attempts. " +
               "Please try rephrasing your question or asking something more specific.";
    }

    /**
     * Build the enriched input for the PlannerAgent with context.
     */
    private String buildPlannerInput(String userQuery, String userId, String sessionId, String feedback) {
        StringBuilder sb = new StringBuilder();

        String currentDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        sb.append("Today's date: ").append(currentDate).append("\n");
        sb.append("User ID: ").append(userId).append("\n\n");

        List<ConversationTurn> history = conversationHistory.get(sessionId);
        if (history != null && !history.isEmpty()) {
            sb.append("Recent conversation:\n");
            for (ConversationTurn turn : history) {
                sb.append("[User]: ").append(turn.userQuery).append("\n");
                String truncatedResponse = turn.assistantResponse;
                if (truncatedResponse.length() > 300) {
                    truncatedResponse = truncatedResponse.substring(0, 297) + "...";
                }
                sb.append("[Assistant]: ").append(truncatedResponse).append("\n\n");
            }
        }

        if (feedback != null && !feedback.isEmpty()) {
            sb.append("IMPORTANT - Previous attempt feedback: ").append(feedback).append("\n\n");
        }

        sb.append("Current user query: ").append(userQuery);
        return sb.toString();
    }

    /**
     * Execute plan steps in parallel, delegating to appropriate sub-agents.
     */
    private Map<String, String> executePlanSteps(JsonNode stepsNode, String userId, String sessionId) {
        Map<String, String> results = new LinkedHashMap<>();
        List<CompletableFuture<Map.Entry<String, String>>> futures = new ArrayList<>();

        int stepCount = Math.min(stepsNode.size(), MAX_PLAN_STEPS);
        long stepTimeoutSeconds = Math.max(15, toolCallTimeoutSeconds * 3L);

        for (int i = 0; i < stepCount; i++) {
            JsonNode step = stepsNode.get(i);
            String agentName = step.path("agent").asText("");
            String task = step.path("task").asText("");

            if (agentName.isEmpty() || task.isEmpty()) {
                results.put("Step " + (i + 1), "{\"error\":\"Invalid plan step: missing agent or task\"}");
                continue;
            }

            final int stepIndex = i;
            final String agentNameFinal = agentName;
            final String taskFinal = task;

            CompletableFuture<Map.Entry<String, String>> future = CompletableFuture.supplyAsync(() -> {
                ToolCallAspect.setSessionId(sessionId);
                try {
                    String key = "Step " + (stepIndex + 1) + " [" + agentNameFinal + "] - " + taskFinal;
                    String result = executeAgentTask(agentNameFinal, taskFinal, userId, sessionId);
                    return Map.entry(key, result);
                } finally {
                    ToolCallAspect.clearSessionId();
                }
            }, agentExecutor);

            futures.add(future);
        }

        for (int i = 0; i < futures.size(); i++) {
            try {
                Map.Entry<String, String> entry = futures.get(i).get(stepTimeoutSeconds, TimeUnit.SECONDS);
                results.put(entry.getKey(), entry.getValue());
            } catch (TimeoutException e) {
                futures.get(i).cancel(true);
                results.put("Step " + (i + 1) + " (timeout)",
                        "{\"error\":\"Agent timed out after " + stepTimeoutSeconds + " seconds\"}");
            } catch (Exception e) {
                results.put("Step " + (i + 1) + " (error)",
                        "{\"error\":\"Agent execution failed: " + e.getMessage() + "\"}");
            }
        }

        return results;
    }

    /**
     * Execute a single agent task by routing to the appropriate sub-agent.
     * Agent name matching is flexible to handle LLM naming variations
     * (e.g., "MARKET_ANALYSIS", "MarketAnalysis", "Market Analysis" all work).
     */
    private String executeAgentTask(String agentName, String task, String userId, String sessionId) {
        String enrichedTask = enrichSubAgentQuery(task, userId);
        String normalized = agentName.toUpperCase().replaceAll("[\\s_\\-]+", "");
        log.info("🔄 [ORCHESTRATOR] Executing: agent={} (normalized={}), task={}", agentName, normalized, task);

        try {
            String result = switch (normalized) {
                case "MARKETANALYSIS", "MARKET" -> marketAnalysisAgent.processQuery(sessionId, enrichedTask);
                case "USERPROFILE", "USER", "PROFILE" -> userProfileAgent.processQuery(sessionId, enrichedTask);
                case "WEBSEARCH", "WEB", "SEARCH" -> webSearchAgent.processQuery(sessionId, enrichedTask);
                case "FINTWIT", "FINTWITANALYSIS", "TWITTER", "SENTIMENT" ->
                        fintwitAnalysisAgent.processQuery(sessionId, enrichedTask);
                default -> {
                    log.warn("⚠️ Unknown agent in plan: {} (normalized: {}). Available: MARKET_ANALYSIS, USER_PROFILE, WEB_SEARCH, FINTWIT",
                            agentName, normalized);
                    yield "{\"error\":\"Unknown agent: " + agentName + ". Available: MARKET_ANALYSIS, USER_PROFILE, WEB_SEARCH, FINTWIT\"}";
                }
            };
            if (result == null || result.trim().isEmpty()) {
                log.warn("⚠️ Agent {} returned null/empty result for task: {}", agentName, task);
                return "{\"error\":\"Agent " + agentName + " returned no data\"}";
            }
            return result;
        } catch (Exception e) {
            log.error("❌ Agent {} threw exception: {}", agentName, e.getMessage(), e);
            return "{\"error\":\"Agent " + agentName + " failed: " + e.getMessage() + "\"}";
        }
    }

    private String enrichSubAgentQuery(String query, String userId) {
        return String.format(
                "Authenticated user id: %s. Use this exact user id for any user profile/portfolio tools. " +
                        "Do not invent user ids.\nUser query: %s",
                userId,
                query
        );
    }

    /**
     * Build the input for the EvaluatorAgent.
     */
    private String buildEvaluationInput(String userQuery, String planJson, Map<String, String> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("ORIGINAL QUERY: ").append(userQuery).append("\n\n");
        sb.append("PLAN: ").append(planJson).append("\n\n");
        sb.append("EXECUTION RESULTS:\n");

        for (Map.Entry<String, String> entry : results.entrySet()) {
            sb.append(entry.getKey()).append(":\n");
            String value = entry.getValue();
            if (value != null && value.length() > 2000) {
                value = value.substring(0, 1997) + "...";
            }
            sb.append(value).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Build a fallback response from raw agent results when the evaluator fails.
     */
    private String buildFallbackResponse(Map<String, String> results) {
        if (results.isEmpty()) {
            return "I apologize, but I wasn't able to retrieve the requested information. Please try again.";
        }

        StringBuilder sb = new StringBuilder("Here's what I found:\n\n");
        for (Map.Entry<String, String> entry : results.entrySet()) {
            sb.append("**").append(entry.getKey()).append("**: ");
            String value = entry.getValue();
            if (value != null && value.length() > 500) {
                value = value.substring(0, 497) + "...";
            }
            sb.append(value).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Store a conversation turn for follow-up context.
     */
    private void addConversationTurn(String sessionId, String userQuery, String response) {
        List<ConversationTurn> history = conversationHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>());

        history.add(new ConversationTurn(userQuery, response));

        while (history.size() > MAX_CONVERSATION_HISTORY) {
            history.remove(0);
        }
    }

    /**
     * Extract JSON from LLM output, handling markdown fences and surrounding text.
     */
    private JsonNode extractJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String trimmed = raw.trim();

        // Try direct parse
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
        }

        // Try stripping markdown code fences
        if (trimmed.contains("```")) {
            String stripped = trimmed
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            try {
                return objectMapper.readTree(stripped);
            } catch (Exception ignored) {
            }
        }

        // Try extracting JSON object from surrounding text
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            try {
                return objectMapper.readTree(trimmed.substring(firstBrace, lastBrace + 1));
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Check agent status - verify all agents are available.
     */
    public Map<String, Boolean> getAgentStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        status.put("plannerAgent", plannerAgent != null);
        status.put("evaluatorAgent", evaluatorAgent != null);
        status.put("userProfileAgent", userProfileAgent != null);
        status.put("marketAnalysisAgent", marketAnalysisAgent != null);
        status.put("webSearchAgent", webSearchAgent != null);
        status.put("fintwitAnalysisAgent", fintwitAnalysisAgent != null);
        status.put("securityAgent", securityAgent != null);
        return status;
    }

    /**
     * Clear session data (conversation history, user ID cache).
     */
    public void clearSession(String sessionId) {
        sessionUserIdCache.remove(sessionId);
        conversationHistory.remove(sessionId);
        log.info("Cleared session data for: {}", sessionId);
    }

    @PreDestroy
    public void shutdownExecutor() {
        agentExecutor.shutdownNow();
    }
}
