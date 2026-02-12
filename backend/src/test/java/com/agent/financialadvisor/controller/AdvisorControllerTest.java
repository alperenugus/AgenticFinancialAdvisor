package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.repository.RecommendationRepository;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdvisorController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrchestratorService orchestratorService;

    @MockBean
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testAnalyze_Success() throws Exception {
        // Given
        String mockResponse = "Based on analysis, I recommend BUY for AAPL";
        when(orchestratorService.coordinateAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        Map<String, String> request = new HashMap<>();
        request.put("userId", "test-user");
        request.put("query", "Should I buy AAPL?");

        // When & Then
        mockMvc.perform(post("/api/advisor/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.response").value(mockResponse));

        verify(orchestratorService, times(1)).coordinateAnalysis(anyString(), anyString(), anyString());
    }

    @Test
    void testAnalyze_MissingQuery() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("userId", "test-user");

        // When & Then
        mockMvc.perform(post("/api/advisor/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orchestratorService, never()).coordinateAnalysis(anyString(), anyString(), anyString());
    }

    @Test
    void testGetRecommendations_Success() throws Exception {
        // Given
        Recommendation rec1 = new Recommendation();
        rec1.setId(1L);
        rec1.setUserId("test-user");
        rec1.setSymbol("AAPL");
        rec1.setAction(Recommendation.RecommendationAction.BUY);

        Recommendation rec2 = new Recommendation();
        rec2.setId(2L);
        rec2.setUserId("test-user");
        rec2.setSymbol("MSFT");
        rec2.setAction(Recommendation.RecommendationAction.HOLD);

        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc("test-user"))
                .thenReturn(Arrays.asList(rec1, rec2));

        // When & Then
        mockMvc.perform(get("/api/advisor/recommendations/test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(recommendationRepository, times(1)).findByUserIdOrderByCreatedAtDesc("test-user");
    }

    @Test
    void testGetStatus_Success() throws Exception {
        // Given
        Map<String, Boolean> agentStatus = new HashMap<>();
        agentStatus.put("userProfileAgent", true);
        agentStatus.put("marketAnalysisAgent", true);

        when(orchestratorService.getAgentStatus()).thenReturn(agentStatus);

        // When & Then
        mockMvc.perform(get("/api/advisor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("operational"))
                .andExpect(jsonPath("$.agents").exists());

        verify(orchestratorService, times(1)).getAgentStatus();
    }
}

