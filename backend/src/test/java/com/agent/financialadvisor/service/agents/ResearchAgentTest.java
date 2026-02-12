package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchAgentTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ResearchAgent researchAgent;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetCompanyFundamentals_Success() throws Exception {
        // Given
        Map<String, Object> overview = new HashMap<>();
        overview.put("symbol", "AAPL");
        overview.put("name", "Apple Inc.");
        overview.put("sector", "Technology");
        overview.put("peRatio", "25.5");

        when(marketDataService.getCompanyOverview("AAPL")).thenReturn(overview);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"symbol\":\"AAPL\"}");

        // When
        String result = researchAgent.getCompanyFundamentals("AAPL");

        // Then
        assertThat(result).isNotNull();
        verify(marketDataService, times(1)).getCompanyOverview("AAPL");
    }

    @Test
    void testAnalyzeFinancials_Success() {
        // Given
        Map<String, Object> fundamentals = new HashMap<>();
        fundamentals.put("symbol", "AAPL");
        fundamentals.put("peRatio", "20.5");
        fundamentals.put("profitMargin", "25.0");
        fundamentals.put("revenueGrowth", "15.0");

        when(marketDataService.getCompanyOverview("AAPL")).thenReturn(fundamentals);

        // When
        String result = researchAgent.analyzeFinancials("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("analysis");
        verify(marketDataService, times(1)).getCompanyOverview("AAPL");
    }

    @Test
    void testCompareCompanies_Success() throws Exception {
        // Given
        Map<String, Object> overview1 = new HashMap<>();
        overview1.put("symbol", "AAPL");
        overview1.put("peRatio", "25.5");

        Map<String, Object> overview2 = new HashMap<>();
        overview2.put("symbol", "MSFT");
        overview2.put("peRatio", "30.0");

        when(marketDataService.getCompanyOverview("AAPL")).thenReturn(overview1);
        when(marketDataService.getCompanyOverview("MSFT")).thenReturn(overview2);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"comparisons\":[]}");

        // When
        String result = researchAgent.compareCompanies("AAPL,MSFT");

        // Then
        assertThat(result).isNotNull();
        verify(marketDataService, atLeastOnce()).getCompanyOverview(anyString());
    }

    @Test
    void testGetSectorAnalysis_Success() throws Exception {
        // Given
        Map<String, Object> overview = new HashMap<>();
        overview.put("symbol", "AAPL");
        overview.put("name", "Apple Inc.");
        overview.put("sector", "Technology");

        when(marketDataService.getCompanyOverview("AAPL")).thenReturn(overview);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"sector\":\"Technology\"}");

        // When
        String result = researchAgent.getSectorAnalysis("AAPL");

        // Then
        assertThat(result).isNotNull();
        verify(marketDataService, times(1)).getCompanyOverview("AAPL");
    }
}

