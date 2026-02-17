package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LlmTickerResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolve_ReturnsAcceptedSymbolWhenEvaluatorAndAuditorPass() {
        LlmTickerResolver resolver = new LlmTickerResolver(
                objectMapper,
                2,
                payload -> "{\"objective\":\"Select best identity match\"}",
                payload -> "{\"symbol\":\"FIG\",\"reason\":\"Best match\"}",
                payload -> "{\"verdict\":\"PASS\",\"reason\":\"Identity verified\"}",
                payload -> "{\"verdict\":\"PASS\",\"reason\":\"Independent audit pass\"}"
        );

        TickerResolver.Decision decision = resolver.resolve("Figma", candidates());

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.symbol()).isEqualTo("FIG");
        assertThat(decision.reason()).contains("Identity verified");
        assertThat(decision.reason()).contains("Independent audit pass");
    }

    @Test
    void resolve_RetriesWhenEvaluatorRejectsFirstAttempt() {
        AtomicInteger selectCounter = new AtomicInteger();
        LlmTickerResolver resolver = new LlmTickerResolver(
                objectMapper,
                3,
                payload -> "{\"objective\":\"Disambiguate lookalike symbols\"}",
                payload -> selectCounter.incrementAndGet() == 1
                        ? "{\"symbol\":\"FIGS\",\"reason\":\"First pick\"}"
                        : "{\"symbol\":\"FIG\",\"reason\":\"Second pick\"}",
                payload -> payload.contains("\"selectedSymbol\":\"FIGS\"")
                        ? "{\"verdict\":\"FAIL\",\"reason\":\"Selected wrong company ticker\"}"
                        : "{\"verdict\":\"PASS\",\"reason\":\"Identity verified\"}",
                payload -> "{\"verdict\":\"PASS\",\"reason\":\"Audit pass\"}"
        );

        TickerResolver.Decision decision = resolver.resolve("Figma", candidates());

        assertThat(selectCounter.get()).isGreaterThanOrEqualTo(2);
        assertThat(decision.accepted()).isTrue();
        assertThat(decision.symbol()).isEqualTo("FIG");
    }

    @Test
    void resolve_FailsWhenAuditorRejectsAllAttempts() {
        LlmTickerResolver resolver = new LlmTickerResolver(
                objectMapper,
                2,
                payload -> "{\"objective\":\"Choose candidate\"}",
                payload -> "{\"symbol\":\"FIG\",\"reason\":\"Selection\"}",
                payload -> "{\"verdict\":\"PASS\",\"reason\":\"Evaluator pass\"}",
                payload -> "{\"verdict\":\"FAIL\",\"reason\":\"Auditor requires higher confidence\"}"
        );

        TickerResolver.Decision decision = resolver.resolve("Figma", candidates());

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.symbol()).isNull();
    }

    @Test
    void resolve_FailsWhenNoCandidatesProvided() {
        LlmTickerResolver resolver = new LlmTickerResolver(
                objectMapper,
                2,
                payload -> "{\"objective\":\"N/A\"}",
                payload -> "{\"symbol\":null}",
                payload -> "{\"verdict\":\"FAIL\",\"reason\":\"No candidates\"}",
                payload -> "{\"verdict\":\"FAIL\",\"reason\":\"No candidates\"}"
        );

        TickerResolver.Decision decision = resolver.resolve("Figma", List.of());

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.symbol()).isNull();
        assertThat(decision.reason()).contains("No candidates");
    }

    private List<TickerResolver.Candidate> candidates() {
        return List.of(
                new TickerResolver.Candidate("FIGS", "FIGS INC", "Common Stock", false),
                new TickerResolver.Candidate("FIG", "Figma Inc", "Common Stock", false)
        );
    }
}
