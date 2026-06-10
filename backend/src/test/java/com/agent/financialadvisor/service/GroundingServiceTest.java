package com.agent.financialadvisor.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingServiceTest {

    private final GroundingService grounding = new GroundingService();

    private static final String TOOL_DATA =
            "{\"symbol\": \"AAPL\", \"price\": 290.55, \"currency\": \"USD\", " +
            "\"quoteTime\": \"2026-06-09T20:00:00Z\", \"gainLossPercent\": 93.70, " +
            "\"totalValue\": 2905.50}";

    @Test
    void passes_whenAllFiguresComeFromToolData() {
        String response = "**AAPL** is at $290.55 USD (as of 2026-06-09T20:00:00Z). " +
                "Your position is worth $2,905.50, up 93.70%.";
        assertThat(grounding.findUngroundedNumbers(response, List.of(TOOL_DATA))).isEmpty();
    }

    @Test
    void flags_inventedPrice() {
        String response = "AAPL is trading at $312.40 right now.";
        List<String> offenders = grounding.findUngroundedNumbers(response, List.of(TOOL_DATA));
        assertThat(offenders).containsExactly("312.40");
    }

    @Test
    void flags_inventedPercentage_butKeepsRealOnes() {
        String response = "AAPL is up 93.70% overall but fell 4.85% today.";
        List<String> offenders = grounding.findUngroundedNumbers(response, List.of(TOOL_DATA));
        assertThat(offenders).containsExactly("4.85");
    }

    @Test
    void toleratesRoundingAndThousandsSeparators() {
        // 93.7 vs source 93.70, and 2,905.50 vs 2905.50
        String response = "Up 93.7%, position value $2,905.50.";
        assertThat(grounding.findUngroundedNumbers(response, List.of(TOOL_DATA))).isEmpty();
    }

    @Test
    void whitelistsSmallCountsAndYears() {
        String response = "You hold 2 stocks. Back in 2024 you started with 3 goals.";
        assertThat(grounding.findUngroundedNumbers(response, List.of("no numbers here"))).isEmpty();
    }

    @Test
    void flags_largeIntegerNotInSources() {
        String response = "The company employs 164000 people.";
        assertThat(grounding.findUngroundedNumbers(response, List.of(TOOL_DATA)))
                .containsExactly("164000");
    }

    @Test
    void emptyResponse_isGrounded() {
        assertThat(grounding.findUngroundedNumbers("", List.of(TOOL_DATA))).isEmpty();
        assertThat(grounding.findUngroundedNumbers(null, List.of(TOOL_DATA))).isEmpty();
    }

    @Test
    void responseWithoutNumbers_isGrounded() {
        assertThat(grounding.findUngroundedNumbers(
                "I could not retrieve that data right now.", List.of())).isEmpty();
    }

    @Test
    void anyFigureIsUngrounded_whenSourcesAreEmpty() {
        // Used by the directResponse gate: a greeting carrying figures must be rejected.
        assertThat(grounding.findUngroundedNumbers("AAPL is around $290.", List.of()))
                .containsExactly("290");
    }
}
