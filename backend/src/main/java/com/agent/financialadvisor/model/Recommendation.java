package com.agent.financialadvisor.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationAction action;

    @Column(nullable = false)
    private Double confidence; // 0.0 - 1.0

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "target_price", precision = 19, scale = 2)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_horizon")
    private InvestmentHorizon timeHorizon;

    @Column(name = "market_analysis", columnDefinition = "TEXT")
    private String marketAnalysis;

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    @Column(name = "research_summary", columnDefinition = "TEXT")
    private String researchSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public RecommendationAction getAction() { return action; }
    public void setAction(RecommendationAction action) { this.action = action; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public InvestmentHorizon getTimeHorizon() { return timeHorizon; }
    public void setTimeHorizon(InvestmentHorizon timeHorizon) { this.timeHorizon = timeHorizon; }
    public String getMarketAnalysis() { return marketAnalysis; }
    public void setMarketAnalysis(String marketAnalysis) { this.marketAnalysis = marketAnalysis; }
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    public String getResearchSummary() { return researchSummary; }
    public void setResearchSummary(String researchSummary) { this.researchSummary = researchSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum RecommendationAction {
        BUY, SELL, HOLD
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public enum InvestmentHorizon {
        SHORT, MEDIUM, LONG
    }
}


