package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.FintwitAnalysisAgent;
import com.agent.financialadvisor.service.agents.MarketAnalysisAgent;
import com.agent.financialadvisor.service.agents.SecurityAgent;
import com.agent.financialadvisor.service.agents.UserProfileAgent;
import com.agent.financialadvisor.service.agents.WebSearchAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrchestratorServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private UserProfileAgent userProfileAgent;

    @Mock
    private MarketAnalysisAgent marketAnalysisAgent;

    @Mock
    private WebSearchAgent webSearchAgent;

    @Mock
    private FintwitAnalysisAgent fintwitAnalysisAgent;

    @Mock
    private SecurityAgent securityAgent;

    @Mock
    private WebSocketService webSocketService;

    private OrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        orchestratorService = new OrchestratorService(
                chatLanguageModel,
                userProfileAgent,
                marketAnalysisAgent,
                webSearchAgent,
                fintwitAnalysisAgent,
                securityAgent,
                webSocketService,
                new ObjectMapper(),
                90,
                10,
                2
        );
    }

    @Test
    void parseEvaluationDecision_ParsesFailJson() {
        String raw = """
                {"verdict":"FAIL","reason":"Ticker does not match requested company","retryInstruction":"Call MarketAnalysisAgent again and verify company identity from tool output."}
                """;

        OrchestratorService.EvaluationDecision decision = orchestratorService.parseEvaluationDecisionForTesting(raw);

        assertThat(decision.isPass()).isFalse();
        assertThat(decision.reason()).contains("Ticker does not match requested company");
        assertThat(decision.retryInstruction()).contains("verify company identity");
    }

    @Test
    void parseEvaluationDecision_ParsesPassJsonInsideCodeFence() {
        String raw = """
                ```json
                {"verdict":"PASS","reason":"Response is grounded in delegated evidence","retryInstruction":""}
                ```
                """;

        OrchestratorService.EvaluationDecision decision = orchestratorService.parseEvaluationDecisionForTesting(raw);

        assertThat(decision.isPass()).isTrue();
        assertThat(decision.reason()).contains("grounded");
    }

    @Test
    void parseEvaluationDecision_HandlesPlainFailText() {
        OrchestratorService.EvaluationDecision decision = orchestratorService.parseEvaluationDecisionForTesting(
                "FAIL: The answer includes unsupported real-time claims."
        );

        assertThat(decision.isPass()).isFalse();
        assertThat(decision.retryInstruction()).contains("unsupported real-time claims");
    }

    @Test
    void parseEvaluationDecision_DefaultsPassForUnparseableOutput() {
        OrchestratorService.EvaluationDecision decision = orchestratorService.parseEvaluationDecisionForTesting(
                "I am not sure."
        );

        assertThat(decision.isPass()).isTrue();
        assertThat(decision.reason()).contains("unclear");
    }
}
