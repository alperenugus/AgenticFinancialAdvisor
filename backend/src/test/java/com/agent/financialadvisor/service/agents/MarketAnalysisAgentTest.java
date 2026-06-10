package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.agent.financialadvisor.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketAnalysisAgentTest {

    @Mock
    private MarketDataService marketDataService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private MarketAnalysisAgent marketAnalysisAgent;

    @BeforeEach
    void setUp() {
        marketAnalysisAgent = new MarketAnalysisAgent(
                marketDataService,
                objectMapper,
                webSocketService,
                chatLanguageModel
        );
    }

    @Test
    void testGetStockPrice_Success() {
        // Given
        MarketDataService.Quote quote = new MarketDataService.Quote(new BigDecimal("150.50"), "finnhub", Instant.now());
        when(marketDataService.resolveSymbol("AAPL")).thenReturn("AAPL");
        when(marketDataService.getQuote("AAPL")).thenReturn(quote);

        // When
        String result = marketAnalysisAgent.getStockPrice("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("150.50");
        assertThat(result).contains("USD");
        assertThat(result).contains("finnhub");
        verify(marketDataService, times(1)).resolveSymbol("AAPL");
        verify(marketDataService, times(1)).getQuote("AAPL");
    }

    @Test
    void testGetStockPrice_WhenPriceIsNull() {
        // Given
        when(marketDataService.resolveSymbol("INVALID")).thenReturn("INVALID");
        when(marketDataService.getQuote("INVALID")).thenReturn(null);

        // When
        String result = marketAnalysisAgent.getStockPrice("INVALID");

        // Then
        assertThat(result).contains("INVALID");
        assertThat(result).contains("error");
        verify(marketDataService, times(1)).resolveSymbol("INVALID");
        verify(marketDataService, times(1)).getQuote("INVALID");
    }

    @Test
    void testGetStockPrice_ResolvesCompanyNameUsingLiveLookup() {
        // Given
        when(marketDataService.resolveSymbol("figma")).thenReturn("FIG");
        when(marketDataService.getQuote("FIG"))
                .thenReturn(new MarketDataService.Quote(new BigDecimal("42.00"), "finnhub", Instant.now()));

        // When
        String result = marketAnalysisAgent.getStockPrice("figma");

        // Then
        assertThat(result).contains("\"requested\": \"figma\"");
        assertThat(result).contains("\"symbol\": \"FIG\"");
        assertThat(result).contains("42.00");
        verify(marketDataService, times(1)).resolveSymbol("figma");
        verify(marketDataService, times(1)).getQuote("FIG");
    }

    @Test
    void testGetStockPrice_WhenSymbolCannotBeResolved() {
        // Given
        when(marketDataService.resolveSymbol("not-a-real-company")).thenReturn(null);

        // When
        String result = marketAnalysisAgent.getStockPrice("not-a-real-company");

        // Then
        assertThat(result).contains("not-a-real-company");
        assertThat(result).contains("error");
        verify(marketDataService, times(1)).resolveSymbol("not-a-real-company");
        verify(marketDataService, never()).getStockPrice(anyString());
    }

    @Test
    void testGetMarketNews_Success() {
        // Given
        String expectedNews = "Apple announces new product. Market reacts positively.";
        when(marketDataService.resolveSymbol("AAPL")).thenReturn("AAPL");
        when(marketDataService.getMarketNews("AAPL")).thenReturn(expectedNews);

        // When
        String result = marketAnalysisAgent.getMarketNews("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains(expectedNews);
        verify(marketDataService, times(1)).resolveSymbol("AAPL");
        verify(marketDataService, times(1)).getMarketNews("AAPL");
    }

    @Test
    void testAnalyzeTrends_ClassifiesUptrendFromMovingAverages() {
        // Given: close > SMA20 > SMA50 → UPTREND per the documented rule
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("symbol", "AAPL");
        snapshot.put("latestClose", new BigDecimal("160.00"));
        snapshot.put("sma20", new BigDecimal("152.00"));
        snapshot.put("sma50", new BigDecimal("147.50"));
        snapshot.put("asOf", "2026-06-09T20:00:00Z");
        snapshot.put("source", "yahoo-finance-daily-candles");

        when(marketDataService.resolveSymbol("AAPL")).thenReturn("AAPL");
        when(marketDataService.getTechnicalSnapshot("AAPL")).thenReturn(snapshot);

        // When
        String result = marketAnalysisAgent.analyzeTrends("AAPL", "daily");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("UPTREND");
        assertThat(result).contains("160.00");
        assertThat(result).contains("152.00");
        assertThat(result).contains("2026-06-09T20:00:00Z"); // freshness propagated
        verify(marketDataService, times(1)).resolveSymbol("AAPL");
        verify(marketDataService, times(1)).getTechnicalSnapshot("AAPL");
    }

    @Test
    void testGetTechnicalIndicators_ReturnsRealIndicatorsWithLiveQuote() {
        // Given
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("symbol", "AAPL");
        snapshot.put("latestClose", new BigDecimal("155.00"));
        snapshot.put("sma20", new BigDecimal("152.00"));
        snapshot.put("rsi14", new BigDecimal("61.30"));
        snapshot.put("week52High", new BigDecimal("199.62"));
        snapshot.put("asOf", "2026-06-09T20:00:00Z");

        when(marketDataService.resolveSymbol("AAPL")).thenReturn("AAPL");
        when(marketDataService.getTechnicalSnapshot("AAPL")).thenReturn(snapshot);
        when(marketDataService.getQuote("AAPL"))
                .thenReturn(new MarketDataService.Quote(new BigDecimal("155.20"), "finnhub", Instant.now()));

        // When
        String result = marketAnalysisAgent.getTechnicalIndicators("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("61.30");   // RSI14
        assertThat(result).contains("199.62");  // 52-week high
        assertThat(result).contains("155.20");  // live quote
        assertThat(result).contains("finnhub"); // quote provenance
        verify(marketDataService, times(1)).resolveSymbol("AAPL");
        verify(marketDataService, times(1)).getTechnicalSnapshot("AAPL");
        verify(marketDataService, times(1)).getQuote("AAPL");
    }
}

