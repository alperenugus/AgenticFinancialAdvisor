package com.agent.financialadvisor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient to handle large API responses
 * Alpha Vantage API responses can be large (especially TIME_SERIES_MONTHLY)
 */
@Configuration
public class WebClientConfig {

    @Value("${market-data.alpha-vantage.timeout-seconds:8}")
    private int timeoutSeconds;

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Increase buffer size to handle large Alpha Vantage responses
        // Default is 256KB, increase to 10MB for large time series data
        final int size = 10 * 1024 * 1024; // 10MB
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        
        return WebClient.builder()
                .exchangeStrategies(strategies);
    }
}

