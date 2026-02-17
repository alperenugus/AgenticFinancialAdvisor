package com.agent.financialadvisor.service.orchestrator;

import com.agent.financialadvisor.service.WebSocketService;
import com.agent.financialadvisor.service.agents.FintwitAnalysisAgent;
import com.agent.financialadvisor.service.agents.MarketAnalysisAgent;
import com.agent.financialadvisor.service.agents.SecurityAgent;
import com.agent.financialadvisor.service.agents.UserProfileAgent;
import com.agent.financialadvisor.service.agents.WebSearchAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

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
                90,
                10,
                2
        );
    }

    @Test
    void evaluateResponseQuality_FailsWhenLiveMarketQueryHasNoMarketDelegation() {
        OrchestratorService.ResponseQualityCheck result = orchestratorService.evaluateResponseQualityForTesting(
                "What is Tesla stock price right now?",
                "Tesla is trading around $300.",
                Set.of()
        );

        assertThat(result.isPass()).isFalse();
        assertThat(result.reason()).contains("Live-data query answered without market-related tool delegation");
    }

    @Test
    void evaluateResponseQuality_PassesWhenLiveMarketQueryUsesMarketDelegation() {
        OrchestratorService.ResponseQualityCheck result = orchestratorService.evaluateResponseQualityForTesting(
                "What is Tesla stock price right now?",
                "Based on the latest market data, Tesla is trading around $300.",
                Set.of("MarketAnalysisAgent")
        );

        assertThat(result.isPass()).isTrue();
    }

    @Test
    void evaluateResponseQuality_FailsWhenPortfolioQueryWithoutUserProfileDelegation() {
        OrchestratorService.ResponseQualityCheck result = orchestratorService.evaluateResponseQualityForTesting(
                "Can you review my portfolio allocation?",
                "Your portfolio appears concentrated in tech.",
                Set.of("MarketAnalysisAgent")
        );

        assertThat(result.isPass()).isFalse();
        assertThat(result.reason()).contains("User-specific query answered without UserProfileAgent delegation");
    }

    @Test
    void evaluateResponseQuality_FailsWhenResponseAdmitsNoRealtimeAccess() {
        OrchestratorService.ResponseQualityCheck result = orchestratorService.evaluateResponseQualityForTesting(
                "Give me the latest Apple stock price.",
                "I don't have access to real-time prices.",
                Set.of("MarketAnalysisAgent")
        );

        assertThat(result.isPass()).isFalse();
        assertThat(result.reason()).contains("missing real-time access");
    }
}
