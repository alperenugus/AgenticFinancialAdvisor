package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.StockHolding;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PortfolioController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioRepository portfolioRepository;

    @MockBean
    private MarketDataService marketDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setUserId("test-user");
        testPortfolio.setHoldings(new ArrayList<>());
        testPortfolio.setTotalValue(BigDecimal.ZERO);
    }

    @Test
    void testGetPortfolio_Success() throws Exception {
        // Given
        when(portfolioRepository.findByUserId("test-user")).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // When & Then
        mockMvc.perform(get("/api/portfolio/test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("test-user"));

        verify(portfolioRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetPortfolio_CreatesNewIfNotExists() throws Exception {
        // Given
        when(portfolioRepository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // When & Then
        mockMvc.perform(get("/api/portfolio/new-user"))
                .andExpect(status().isOk());

        verify(portfolioRepository, times(1)).findByUserId("new-user");
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testAddHolding_Success() throws Exception {
        // Given
        when(portfolioRepository.findByUserId("test-user")).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
        when(marketDataService.getStockPrice("AAPL")).thenReturn(new BigDecimal("150.00"));

        Map<String, Object> request = new HashMap<>();
        request.put("symbol", "AAPL");
        request.put("quantity", 10);
        request.put("averagePrice", "150.00");

        // When & Then
        mockMvc.perform(post("/api/portfolio/test-user/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Holding added successfully"));

        verify(portfolioRepository, times(1)).findByUserId("test-user");
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }

    @Test
    void testRemoveHolding_Success() throws Exception {
        // Given
        StockHolding holding = new StockHolding();
        holding.setId(1L);
        holding.setSymbol("AAPL");
        testPortfolio.setHoldings(Arrays.asList(holding));

        when(portfolioRepository.findByUserId("test-user")).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // When & Then
        mockMvc.perform(delete("/api/portfolio/test-user/holdings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Holding removed successfully"));

        verify(portfolioRepository, times(1)).findByUserId("test-user");
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testRefreshPortfolio_Success() throws Exception {
        // Given
        when(portfolioRepository.findByUserId("test-user")).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // When & Then
        mockMvc.perform(post("/api/portfolio/test-user/refresh"))
                .andExpect(status().isOk());

        verify(portfolioRepository, times(1)).findByUserId("test-user");
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }
}

