package com.agent.financialadvisor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Configuration to handle proxy headers correctly in Railway/cloud deployments.
 * This ensures Spring Boot correctly detects HTTPS scheme and host from proxy headers.
 */
@Configuration
public class ProxyConfig {

    /**
     * ForwardedHeaderFilter processes X-Forwarded-* headers from proxies.
     * This is essential for Railway deployments where the app is behind a load balancer.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}

