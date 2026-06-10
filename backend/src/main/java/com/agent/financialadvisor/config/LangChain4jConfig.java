package com.agent.financialadvisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * LLM Configuration for OpenAI.
 *
 * The application talks to OpenAI's Chat Completions API through LangChain4j's OpenAI client.
 * Three model tiers, each independently override-able via environment variables:
 *   - orchestrator (planner + evaluator)       — the quality-critical role (default gpt-4o)
 *   - agent (market / profile / web / fintwit)  — tool-calling sub-agents     (default gpt-4o)
 *   - security (input classification, no tools) — cheap + fast                (default gpt-4o-mini)
 *
 * Set OPENAI_API_KEY in the environment. Beans are @Lazy so a missing key doesn't crash startup;
 * the failure surfaces on the first LLM call instead.
 */
@Lazy
@Configuration
public class LangChain4jConfig {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    // Orchestrator (planner + evaluator)
    @Value("${openai.orchestrator.model:gpt-4o}")
    private String orchestratorModel;

    @Value("${openai.orchestrator.temperature:0.0}")
    private Double orchestratorTemperature;

    @Value("${openai.orchestrator.timeout-seconds:90}")
    private Integer orchestratorTimeoutSeconds;

    // Sub-agents (tool calling)
    @Value("${openai.agent.model:gpt-4o}")
    private String agentModel;

    @Value("${openai.agent.temperature:0.0}")
    private Double agentTemperature;

    @Value("${openai.agent.timeout-seconds:60}")
    private Integer agentTimeoutSeconds;

    // Security gate (cheap classification)
    @Value("${openai.security.model:gpt-4o-mini}")
    private String securityModel;

    @Value("${openai.security.temperature:0.0}")
    private Double securityTemperature;

    @Value("${openai.security.timeout-seconds:20}")
    private Integer securityTimeoutSeconds;

    private void requireApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY environment variable is required. " +
                "Set it in Railway environment variables (or your local environment)."
            );
        }
    }

    private ChatLanguageModel build(String model, Double temperature, Integer timeoutSeconds) {
        requireApiKey();
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Orchestrator model — used by the OrchestratorService, PlannerAgent, and EvaluatorAgent.
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        return build(orchestratorModel, orchestratorTemperature, orchestratorTimeoutSeconds);
    }

    /**
     * Sub-agent model — used by MarketAnalysis, UserProfile, WebSearch, and Fintwit agents for tool calling.
     */
    @Bean(name = "agentChatLanguageModel")
    public ChatLanguageModel agentChatLanguageModel() {
        return build(agentModel, agentTemperature, agentTimeoutSeconds);
    }

    /**
     * Lightweight model — used by the SecurityAgent for cheap, fast input classification (no tools).
     * (Bean name kept as "toolAgentChatLanguageModel" so existing @Qualifier injection points are unchanged.)
     */
    @Bean(name = "toolAgentChatLanguageModel")
    public ChatLanguageModel toolAgentChatLanguageModel() {
        return build(securityModel, securityTemperature, securityTimeoutSeconds);
    }
}
