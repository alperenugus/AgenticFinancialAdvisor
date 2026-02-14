package com.agent.financialadvisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * LangChain4j Configuration for Groq API
 * 
 * Groq provides fast, cost-effective LLM inference via OpenAI-compatible API.
 * We use two models:
 * - Orchestrator: llama-3.3-70b for high-level thinking and planning
 * - Tool Agent: llama-3.1-8b for fast, cheap function calling
 */
@Lazy  // Delay Groq connection until first use (prevents blocking startup)
@Configuration
public class LangChain4jConfig {

    // Groq Orchestrator Configuration (llama-3.3-70b)
    @Value("${langchain4j.groq.orchestrator.api-key:}")
    private String orchestratorApiKey;

    @Value("${langchain4j.groq.orchestrator.base-url:https://api.groq.com/openai/v1}")
    private String orchestratorBaseUrl;

    @Value("${langchain4j.groq.orchestrator.model:llama-3.3-70b-versatile}")
    private String orchestratorModel;

    @Value("${langchain4j.groq.orchestrator.temperature:0.7}")
    private Double orchestratorTemperature;

    @Value("${langchain4j.groq.orchestrator.timeout-seconds:60}")
    private Integer orchestratorTimeoutSeconds;

    // Groq Tool Agent Configuration (llama-3.1-8b)
    @Value("${langchain4j.groq.tool-agent.api-key:}")
    private String toolAgentApiKey;

    @Value("${langchain4j.groq.tool-agent.base-url:https://api.groq.com/openai/v1}")
    private String toolAgentBaseUrl;

    @Value("${langchain4j.groq.tool-agent.model:llama-3.1-8b-instant}")
    private String toolAgentModel;

    @Value("${langchain4j.groq.tool-agent.temperature:0.3}")
    private Double toolAgentTemperature;

    @Value("${langchain4j.groq.tool-agent.timeout-seconds:30}")
    private Integer toolAgentTimeoutSeconds;

    /**
     * Orchestrator ChatLanguageModel - uses llama-3.3-70b for high-level thinking
     * This is the primary model used by the orchestrator service
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        if (orchestratorApiKey == null || orchestratorApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "GROQ_API_KEY environment variable is required. " +
                "Please set it in Railway environment variables or application.yml"
            );
        }

        return OpenAiChatModel.builder()
                .apiKey(orchestratorApiKey)
                .baseUrl(orchestratorBaseUrl)
                .modelName(orchestratorModel)
                .temperature(orchestratorTemperature)
                .timeout(java.time.Duration.ofSeconds(orchestratorTimeoutSeconds))
                .logRequests(true)  // Enable logging to debug function call issues
                .logResponses(true)
                .build();
    }

    /**
     * Tool Agent ChatLanguageModel - uses llama-3.1-8b for fast function calling
     * Currently not used, but available for future optimization where tool calls
     * could be made by a separate, cheaper model
     */
    @Bean(name = "toolAgentChatLanguageModel")
    public ChatLanguageModel toolAgentChatLanguageModel() {
        if (toolAgentApiKey == null || toolAgentApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "GROQ_API_KEY environment variable is required. " +
                "Please set it in Railway environment variables or application.yml"
            );
        }

        return OpenAiChatModel.builder()
                .apiKey(toolAgentApiKey)
                .baseUrl(toolAgentBaseUrl)
                .modelName(toolAgentModel)
                .temperature(toolAgentTemperature)
                .timeout(java.time.Duration.ofSeconds(toolAgentTimeoutSeconds))
                .build();
    }
}


