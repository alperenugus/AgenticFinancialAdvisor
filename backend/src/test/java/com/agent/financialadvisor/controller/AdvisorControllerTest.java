package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.service.RateLimitService;
import com.agent.financialadvisor.service.orchestrator.OrchestratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdvisorController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrchestratorService orchestratorService;

    @MockBean
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("test-user", "n/a");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAnalyze_Success() throws Exception {
        String mockResponse = "Based on analysis, I recommend BUY for AAPL";
        when(orchestratorService.coordinateAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);
        when(rateLimitService.getRemainingAdvisorTokens(anyString())).thenReturn(19);

        Map<String, String> request = new HashMap<>();
        request.put("query", "Should I buy AAPL?");
        request.put("sessionId", "session-1");

        mockMvc.perform(post("/api/advisor/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.response").value(mockResponse))
                .andExpect(jsonPath("$.userId").value("test-user"));

        verify(orchestratorService, times(1))
                .coordinateAnalysis("test-user", "Should I buy AAPL?", "session-1");
        verify(rateLimitService, times(1)).checkAdvisorRateLimit("session-1");
    }

    @Test
    void testAnalyze_MissingQuery() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("sessionId", "session-1");

        mockMvc.perform(post("/api/advisor/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orchestratorService, never()).coordinateAnalysis(anyString(), anyString(), anyString());
        verify(rateLimitService, never()).checkAdvisorRateLimit(anyString());
    }

    @Test
    void testGetStatus_Success() throws Exception {
        Map<String, Boolean> agentStatus = new HashMap<>();
        agentStatus.put("userProfileAgent", true);
        agentStatus.put("marketAnalysisAgent", true);

        when(orchestratorService.getAgentStatus()).thenReturn(agentStatus);

        mockMvc.perform(get("/api/advisor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("operational"))
                .andExpect(jsonPath("$.agents").exists());

        verify(orchestratorService, times(1)).getAgentStatus();
    }
}

