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

    @Value("${langchain4j.ollama.model:llama3.2:1b}")
    private String ollamaModel;

    @Value("${langchain4j.ollama.temperature:0.7}")
    private Double ollamaTemperature;

    @Value("${langchain4j.ollama.timeout-seconds:300}")
    private Integer timeoutSeconds;

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
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
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
        
        // If URL doesn't start with http:// or https://, add appropriate scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Internal Railway URLs should use http:// (not https://)
            if (url.contains(".railway.internal")) {
                url = "http://" + url;
            } 
            // If it contains a port number, likely internal or local - use http://
            else if (url.contains(":11434") || url.contains(":8080") || url.contains("localhost")) {
                url = "http://" + url;
            }
            // External domains without port - use https://
            else if (url.contains(".")) {
                url = "https://" + url;
            } 
            // Otherwise assume localhost, use http://
            else {
                url = "http://" + url;
            }
        }
        
        // Ensure internal Railway URLs use http:// (not https://)
        if (url.contains(".railway.internal") && url.startsWith("https://")) {
            url = url.replace("https://", "http://");
        }
        
        return url;
    }
}

