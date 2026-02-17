package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataServiceSymbolSelectionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(
                WebClient.builder(),
                objectMapper,
                "",
                "https://finnhub.io/api/v1",
                10
        );
    }

    @Test
    void selectBestSymbolCandidate_PrefersExactCompanyNameMatch() throws Exception {
        JsonNode results = objectMapper.readTree("""
                [
                  {"description":"FIGS INC","displaySymbol":"FIGS","symbol":"FIGS","type":"Common Stock"},
                  {"description":"Figma Inc","displaySymbol":"FIG","symbol":"FIG","type":"Common Stock"}
                ]
                """);

        String resolved = marketDataService.selectBestSymbolCandidateForTesting(results, "Figma");

        assertThat(resolved).isEqualTo("FIG");
    }

    @Test
    void selectBestSymbolCandidate_RejectsLowConfidenceLookalike() throws Exception {
        JsonNode results = objectMapper.readTree("""
                [
                  {"description":"FIGS INC","displaySymbol":"FIGS","symbol":"FIGS","type":"Common Stock"},
                  {"description":"FIGEAC AEROSPACE","displaySymbol":"FIGE","symbol":"FIGE","type":"Common Stock"}
                ]
                """);

        String resolved = marketDataService.selectBestSymbolCandidateForTesting(results, "Figma");

        assertThat(resolved).isNull();
    }

    @Test
    void selectBestSymbolCandidate_UsesExactTickerWhenProvided() throws Exception {
        JsonNode results = objectMapper.readTree("""
                [
                  {"description":"Tesla Inc","displaySymbol":"TSLA","symbol":"TSLA","type":"Common Stock"},
                  {"description":"Tesla Leverage Note","displaySymbol":"TSLAX","symbol":"TSLAX","type":"EQS"}
                ]
                """);

        String resolved = marketDataService.selectBestSymbolCandidateForTesting(results, "TSLA");

        assertThat(resolved).isEqualTo("TSLA");
    }
}
