package com.agent.financialadvisor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${CORS_ORIGINS:*}")
    private String corsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Always use allowedOriginPatterns to avoid conflicts with allowCredentials
        // This works for both wildcard and specific origins
        
        if (corsOrigins.equals("*") || corsOrigins.contains("*")) {
            // Wildcard - don't allow credentials (browser security restriction)
            registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
        } else {
            // Specific origins - use allowedOriginPatterns (not allowedOrigins) to avoid conflicts
            // allowedOriginPatterns supports multiple patterns and works with allowCredentials
            String[] origins = corsOrigins.split(",");
            String[] trimmedOrigins = new String[origins.length];
            for (int i = 0; i < origins.length; i++) {
                trimmedOrigins[i] = origins[i].trim();
            }
            registry.addMapping("/api/**")
                    .allowedOriginPatterns(trimmedOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
        }
        
        // WebSocket CORS - always use wildcard patterns (no credentials needed)
        registry.addMapping("/ws/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
        
        // SockJS info endpoint
        registry.addMapping("/ws/info/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

