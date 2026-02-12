package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.StockHolding;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.agent.financialadvisor.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentAgentTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private RiskAssessmentAgent riskAssessmentAgent;

    private UserProfile testProfile;
    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testProfile = new UserProfile();
        testProfile.setUserId("test-user");
        testProfile.setRiskTolerance(UserProfile.RiskTolerance.MODERATE);

        testPortfolio = new Portfolio();
        testPortfolio.setUserId("test-user");
        testPortfolio.setId(1L);

        StockHolding holding = new StockHolding();
        holding.setId(1L);
        holding.setPortfolioId(1L);
        holding.setSymbol("AAPL");
        holding.setQuantity(10);
        holding.setAveragePrice(new BigDecimal("150.00"));
        holding.setCurrentPrice(new BigDecimal("155.00"));

        testPortfolio.setHoldings(Arrays.asList(holding));
    }

    @Test
    void testAssessStockRisk_Success() {
        // Given
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));

        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(marketDataService.getStockPriceData("AAPL", "daily")).thenReturn(priceData);

        // When
        String result = riskAssessmentAgent.assessStockRisk("AAPL", "");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("riskLevel");
        assertThat(result).contains("volatility");
        verify(marketDataService, times(1)).getStockPrice("AAPL");
        verify(marketDataService, times(1)).getStockPriceData("AAPL", "daily");
    }

    @Test
    void testCalculatePortfolioRisk_Success() {
        // Given
        when(portfolioRepository.findByUserId("test-user")).thenReturn(Optional.of(testPortfolio));

        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));

        when(marketDataService.getStockPrice(anyString())).thenReturn(new BigDecimal("155.00"));
        when(marketDataService.getStockPriceData(anyString(), anyString())).thenReturn(priceData);

        // When
        String result = riskAssessmentAgent.calculatePortfolioRisk("test-user");

        // Then
        assertThat(result).contains("test-user");
        assertThat(result).contains("portfolioRiskLevel");
        assertThat(result).contains("totalHoldings");
        verify(portfolioRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testCalculatePortfolioRisk_WhenPortfolioNotFound() {
        // Given
        when(portfolioRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When
        String result = riskAssessmentAgent.calculatePortfolioRisk("non-existent");

        // Then
        assertThat(result).contains("error");
        assertThat(result).contains("not found");
    }

    @Test
    void testCheckRiskTolerance_Success() {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));

        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(marketDataService.getStockPriceData("AAPL", "daily")).thenReturn(priceData);

        // When
        String result = riskAssessmentAgent.checkRiskTolerance("test-user", "AAPL");

        // Then
        assertThat(result).contains("test-user");
        assertThat(result).contains("AAPL");
        assertThat(result).contains("compatible");
        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetRiskMetrics_Success() {
        // Given
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("high", new BigDecimal("160.00"));
        priceData.put("low", new BigDecimal("140.00"));
        priceData.put("average", new BigDecimal("150.00"));

        when(marketDataService.getStockPriceData("AAPL", "daily")).thenReturn(priceData);
        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("155.00"));

        // When
        String result = riskAssessmentAgent.getRiskMetrics("AAPL");

        // Then
        assertThat(result).contains("AAPL");
        assertThat(result).contains("volatility");
        assertThat(result).contains("beta");
        verify(marketDataService, times(1)).getStockPriceData("AAPL", "daily");
        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }
}

