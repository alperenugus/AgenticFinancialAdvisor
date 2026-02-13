package com.agent.financialadvisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy  // Delay Ollama connection until first use (prevents blocking startup)
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
        
        // Validate and fix URL if needed
        String baseUrl = normalizeOllamaUrl(ollamaBaseUrl);
        
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(ollamaModel)
                .temperature(ollamaTemperature)
                .build();
    }
    
    /**
     * Normalize Ollama URL - ensure it has http:// or https:// scheme
     */
    private String normalizeOllamaUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "http://localhost:11434";
        }
        
        url = url.trim();
        
        // If URL doesn't start with http:// or https://, add https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // If it looks like a domain (contains .), use https://
            if (url.contains(".")) {
                url = "https://" + url;
            } else {
                // Otherwise assume localhost, use http://
                url = "http://" + url;
            }
        }
        
        return url;
    }
}

