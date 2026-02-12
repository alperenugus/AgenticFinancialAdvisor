package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketAnalysisAgentTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MarketAnalysisAgent marketAnalysisAgent;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetStockPrice_Success() {
        // Given
        BigDecimal expectedPrice = new BigDecimal("150.50");
        when(marketDataService.getStockPrice("AAPL")).thenReturn(expectedPrice);

        // When
        String result = marketAnalysisAgent.getStockPrice("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("150.50");
        assertThat(result).contains("USD");
        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }

    @Test
    void testGetStockPrice_WhenPriceIsNull() {
        // Given
        when(marketDataService.getStockPrice("INVALID")).thenReturn(null);

        // When
        String result = marketAnalysisAgent.getStockPrice("INVALID");

        // Then
        assertThat(result).contains("INVALID");
        assertThat(result).contains("error");
        verify(marketDataService, times(1)).getStockPrice("INVALID");
    }

    @Test
    void testGetMarketNews_Success() {
        // Given
        String expectedNews = "Apple announces new product. Market reacts positively.";
        when(marketDataService.getMarketNews("AAPL")).thenReturn(expectedNews);

        // When
        String result = marketAnalysisAgent.getMarketNews("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains(expectedNews);
        verify(marketDataService, times(1)).getMarketNews("AAPL");
    }

    @Test
    void testAnalyzeTrends_Success() {
        // Given
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));
        priceData.put("symbol", "AAPL");

        when(marketDataService.getStockPriceData("AAPL", "daily")).thenReturn(priceData);
        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("155.00"));

        // When
        String result = marketAnalysisAgent.analyzeTrends("AAPL", "daily");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("daily");
        assertThat(result).contains("160.00");
        assertThat(result).contains("140.00");
        verify(marketDataService, times(1)).getStockPriceData("AAPL", "daily");
    }

    @Test
    void testGetTechnicalIndicators_Success() {
        // Given
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));

        when(marketDataService.getStockPriceData("AAPL", "daily")).thenReturn(priceData);
        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("155.00"));

        // When
        String result = marketAnalysisAgent.getTechnicalIndicators("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("155.00");
        verify(marketDataService, times(1)).getStockPriceData("AAPL", "daily");
        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }
}

