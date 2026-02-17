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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                10
        );
    }

    @Test
    void coordinateAnalysis_UsesDirectFlowForSimpleStockPriceQuestion() {
        when(securityAgent.validateInput(anyString()))
                .thenReturn(new SecurityAgent.SecurityValidationResult(true, "SAFE"));
        when(marketAnalysisAgent.getStockPrice("figma"))
                .thenReturn("{\"requested\":\"figma\",\"symbol\":\"FIG\",\"price\":\"42.00\",\"currency\":\"USD\",\"fetchedAt\":\"2026-02-16T00:00:00\"}");

        String result = orchestratorService.coordinateAnalysis("user-1", "figma stock price", "session-1");

        assertThat(result).contains("figma (FIG)");
        assertThat(result).contains("$42.00");
        verify(marketAnalysisAgent).getStockPrice("figma");
        verify(marketAnalysisAgent, never()).processQuery(anyString(), anyString());
        verify(webSocketService).sendFinalResponse(eq("session-1"), contains("FIG"));
    }

    @Test
    void coordinateAnalysis_ReturnsFriendlyErrorWhenDirectToolFails() {
        when(securityAgent.validateInput(anyString()))
                .thenReturn(new SecurityAgent.SecurityValidationResult(true, "SAFE"));
        when(marketAnalysisAgent.getStockPrice("figma"))
                .thenReturn("{\"requested\":\"figma\",\"error\":\"Unable to fetch stock price\"}");

        String result = orchestratorService.coordinateAnalysis("user-1", "stock price of figma", "session-2");

        assertThat(result).contains("couldn't fetch a live stock price");
        verify(marketAnalysisAgent).getStockPrice("figma");
        verify(marketAnalysisAgent, never()).processQuery(anyString(), anyString());
        verify(webSocketService).sendFinalResponse(eq("session-2"), contains("couldn't fetch"));
    }
}
