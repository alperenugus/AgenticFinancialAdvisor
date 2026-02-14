package com.agent.financialadvisor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration to validate OAuth2 credentials at startup
 */
@Configuration
public class OAuth2Config {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Config.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String clientSecret;

    @PostConstruct
    public void validateOAuth2Config() {
        if (clientId == null || clientId.trim().isEmpty()) {
            log.error("❌ GOOGLE_CLIENT_ID is not set or is empty!");
            log.error("   Please set GOOGLE_CLIENT_ID environment variable in Railway");
            log.error("   This will cause 'Invalid credentials' errors");
        } else {
            log.info("✅ GOOGLE_CLIENT_ID is set: {}...{}", 
                clientId.substring(0, Math.min(10, clientId.length())),
                clientId.substring(Math.max(0, clientId.length() - 10)));
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            log.error("❌ GOOGLE_CLIENT_SECRET is not set or is empty!");
            log.error("   Please set GOOGLE_CLIENT_SECRET environment variable in Railway");
            log.error("   This will cause 'Invalid credentials' errors");
        } else {
            log.info("✅ GOOGLE_CLIENT_SECRET is set: {}...{}", 
                clientSecret.substring(0, Math.min(10, clientSecret.length())),
                clientSecret.substring(Math.max(0, clientSecret.length() - 10)));
        }

        if ((clientId == null || clientId.trim().isEmpty()) || 
            (clientSecret == null || clientSecret.trim().isEmpty())) {
            log.error("⚠️  OAuth2 authentication will not work until both GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are set!");
        }
    }
}

