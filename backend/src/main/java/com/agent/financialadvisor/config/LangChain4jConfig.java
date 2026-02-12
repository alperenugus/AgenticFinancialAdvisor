package com.agent.financialadvisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    // Ollama configuration
    // For local: http://localhost:11434
    // For Railway/production: https://your-ollama-service.railway.app
    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model:llama3.1}")
    private String ollamaModel;

    @Value("${langchain4j.ollama.temperature:0.7}")
    private Double ollamaTemperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Use Ollama - can be local or deployed on Railway/cloud
        // Local: http://localhost:11434 (run: ollama serve)
        // Railway: https://your-ollama-service.railway.app
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .temperature(ollamaTemperature)
                .build();
    }
}

