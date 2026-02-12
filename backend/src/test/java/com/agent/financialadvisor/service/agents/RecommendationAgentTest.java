package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationAgentTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationAgent recommendationAgent;

    private Recommendation savedRecommendation;

    @BeforeEach
    void setUp() {
        savedRecommendation = new Recommendation();
        savedRecommendation.setId(1L);
        savedRecommendation.setUserId("test-user");
        savedRecommendation.setSymbol("AAPL");
        savedRecommendation.setAction(Recommendation.RecommendationAction.BUY);
        savedRecommendation.setConfidence(0.8);
    }

    @Test
    void testGenerateRecommendation_Success() {
        // Given
        String marketAnalysis = "Uptrend detected, bullish signals";
        String riskAssessment = "Low risk, stable volatility";
        String researchSummary = "Strong fundamentals, undervalued";
        String userProfile = "MODERATE risk tolerance";

        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(savedRecommendation);

        // When
        String result = recommendationAgent.generateRecommendation(
                "test-user", "AAPL", marketAnalysis, riskAssessment, researchSummary, userProfile
        );

        // Then
        assertThat(result).contains("test-user");
        assertThat(result).contains("AAPL");
        assertThat(result).contains("action");
        verify(recommendationRepository, times(1)).save(any(Recommendation.class));
    }

    @Test
    void testExplainReasoning_Success() {
        // Given
        String components = "{\"marketAnalysis\":\"uptrend\",\"riskAssessment\":\"low risk\",\"researchSummary\":\"strong\"}";

        // When
        String result = recommendationAgent.explainReasoning(components);

        // Then
        assertThat(result).contains("explanation");
        assertThat(result).contains("Reasoning");
    }

    @Test
    void testCalculateConfidence_Success() {
        // Given
        String factors = "{\"marketSignal\":\"bullish\",\"riskLevel\":\"low\",\"fundamentals\":\"good\"}";

        // When
        String result = recommendationAgent.calculateConfidence(factors);

        // Then
        assertThat(result).contains("confidence");
        assertThat(result).contains("explanation");
    }

    @Test
    void testFormatRecommendation_Success() {
        // Given
        String recommendation = "{\"action\":\"BUY\",\"symbol\":\"AAPL\",\"confidence\":0.8,\"reasoning\":\"Strong buy\"}";

        // When
        String result = recommendationAgent.formatRecommendation(recommendation);

        // Then
        assertThat(result).contains("formatted");
    }
}

