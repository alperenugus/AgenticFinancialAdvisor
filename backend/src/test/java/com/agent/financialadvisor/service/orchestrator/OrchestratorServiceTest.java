package com.agent.financialadvisor.service.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrchestratorServiceTest {

    private EvaluatorDecisionParser parser;

    @BeforeEach
    void setUp() {
        parser = new EvaluatorDecisionParser(new ObjectMapper());
    }

    @Test
    void parse_FailJson() {
        String raw = """
                {"verdict":"FAIL","reason":"Ticker does not match requested company","retryInstruction":"Call MarketAnalysisAgent and verify company identity from evidence."}
                """;

        EvaluatorDecisionParser.EvaluationDecision decision = parser.parse(raw);

        assertThat(decision.isPass()).isFalse();
        assertThat(decision.getReason()).contains("Ticker does not match");
        assertThat(decision.getRetryInstruction()).contains("verify company identity");
    }

    @Test
    void parse_PassJsonWithinCodeFence() {
        String raw = """
                ```json
                {"verdict":"PASS","reason":"Response is grounded","retryInstruction":""}
                ```
                """;

        EvaluatorDecisionParser.EvaluationDecision decision = parser.parse(raw);

        assertThat(decision.isPass()).isTrue();
        assertThat(decision.getReason()).contains("grounded");
    }

    @Test
    void parse_PlainFailText() {
        EvaluatorDecisionParser.EvaluationDecision decision = parser.parse(
                "FAIL: Unsupported real-time claims."
        );

        assertThat(decision.isPass()).isFalse();
        assertThat(decision.getRetryInstruction()).contains("Unsupported real-time claims");
    }

    @Test
    void parse_UnknownTextDefaultsPass() {
        EvaluatorDecisionParser.EvaluationDecision decision = parser.parse("Not sure.");

        assertThat(decision.isPass()).isTrue();
        assertThat(decision.getReason()).contains("unclear");
    }
}
