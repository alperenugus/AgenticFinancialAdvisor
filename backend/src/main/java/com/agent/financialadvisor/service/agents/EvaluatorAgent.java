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
 * Evaluator Agent - Reviews execution results and synthesizes user-facing responses.
 * Uses the orchestrator LLM (70B) for strong reasoning about result quality and response synthesis.
 * Provides self-correction by requesting retries when results are insufficient.
 */
@Service
public class EvaluatorAgent {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorAgent.class);
    private final EvaluatorService evaluatorService;

    @Autowired
    public EvaluatorAgent(ChatLanguageModel chatLanguageModel) {
        this.evaluatorService = AiServices.builder(EvaluatorService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
        log.info("✅ EvaluatorAgent initialized with orchestrator LLM");
    }

    /**
     * Evaluate execution results and either synthesize a final response or request a retry.
     *
     * @param evaluationInput Formatted string with original query, plan, and execution results
     * @return JSON string with verdict (PASS/RETRY), response, and feedback
     */
    public String evaluate(String evaluationInput) {
        log.info("🔍 [EVALUATOR] Evaluating execution results");
        long startTime = System.currentTimeMillis();
        try {
            String result = evaluatorService.evaluate(evaluationInput);
            long duration = System.currentTimeMillis() - startTime;
            log.info("🔍 [EVALUATOR] Evaluation completed in {}ms", duration);
            return result;
        } catch (Exception e) {
            log.error("❌ [EVALUATOR] Failed to evaluate: {}", e.getMessage(), e);
            throw e;
        }
    }

    private interface EvaluatorService {
        @SystemMessage(
            "You are an Evaluator Agent for an AI Financial Advisor system.\n\n" +
            "You receive the user's original query, the execution plan, and results from each agent step.\n\n" +
            "### YOUR JOB:\n" +
            "1. Check if the execution results adequately answer the user's question\n" +
            "2. If YES (PASS): Synthesize all results into a professional, user-friendly response\n" +
            "3. If NO (RETRY): Identify what's missing and provide specific feedback for a retry\n\n" +
            "### RESPONSE FORMAT (strict JSON, no markdown fences, no extra text):\n" +
            "For PASS:\n" +
            "{\n" +
            "  \"verdict\": \"PASS\",\n" +
            "  \"response\": \"The synthesized user-facing response here\",\n" +
            "  \"feedback\": null\n" +
            "}\n\n" +
            "For RETRY:\n" +
            "{\n" +
            "  \"verdict\": \"RETRY\",\n" +
            "  \"response\": null,\n" +
            "  \"feedback\": \"What went wrong and what to try differently\"\n" +
            "}\n\n" +
            "### PASS RESPONSE GUIDELINES:\n" +
            "- Be professional, concise, and helpful\n" +
            "- Use markdown formatting: **bold** for emphasis, bullet points for lists\n" +
            "- Include specific numbers, prices, and data from the execution results\n" +
            "- Address the user directly\n" +
            "- For simple queries (like a stock price), keep it brief - 1 to 3 sentences\n" +
            "- For complex analysis, provide structured sections with headers\n" +
            "- NEVER fabricate data - only use information present in the execution results\n" +
            "- If some agent results have errors but enough data exists to answer, still PASS with the available data\n" +
            "- Note data freshness limitations when relevant (e.g., 15-minute delay on free tier)\n" +
            "- For stock prices, always include the ticker symbol and currency (USD)\n\n" +
            "### RETRY GUIDELINES:\n" +
            "- Only RETRY if the results completely fail to answer the core question\n" +
            "- Provide specific, actionable feedback: which agents to try, what approach to use\n" +
            "- Do NOT retry for minor missing details or partial data\n" +
            "- Do NOT retry if at least one agent provided useful, relevant data\n" +
            "- Prefer PASS with partial data over RETRY in most cases\n\n" +
            "ALWAYS respond with valid JSON only. No markdown code fences around the JSON."
        )
        String evaluate(@UserMessage String evaluationInput);
    }
}
