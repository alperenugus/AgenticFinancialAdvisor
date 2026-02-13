package com.agent.financialadvisor.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId; // Session-based or actual user ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskTolerance riskTolerance = RiskTolerance.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentHorizon horizon = InvestmentHorizon.MEDIUM;

    @ElementCollection
    @CollectionTable(name = "user_goals", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "goal")
    private List<String> goals = new ArrayList<>(); // RETIREMENT, GROWTH, INCOME

    @Column(precision = 19, scale = 2)
    private BigDecimal budget;

    @ElementCollection
    @CollectionTable(name = "preferred_sectors", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "sector")
    private List<String> preferredSectors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "excluded_sectors", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "sector")
    private List<String> excludedSectors = new ArrayList<>();

    @Column(name = "ethical_investing")
    private Boolean ethicalInvesting = false; // ESG preferences

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public RiskTolerance getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(RiskTolerance riskTolerance) { this.riskTolerance = riskTolerance; }
    public InvestmentHorizon getHorizon() { return horizon; }
    public void setHorizon(InvestmentHorizon horizon) { this.horizon = horizon; }
    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = goals; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public List<String> getPreferredSectors() { return preferredSectors; }
    public void setPreferredSectors(List<String> preferredSectors) { this.preferredSectors = preferredSectors; }
    public List<String> getExcludedSectors() { return excludedSectors; }
    public void setExcludedSectors(List<String> excludedSectors) { this.excludedSectors = excludedSectors; }
    public Boolean getEthicalInvesting() { return ethicalInvesting; }
    public void setEthicalInvesting(Boolean ethicalInvesting) { this.ethicalInvesting = ethicalInvesting; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum RiskTolerance {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }

    public enum InvestmentHorizon {
        SHORT,    // < 1 year
        MEDIUM,   // 1-5 years
        LONG      // > 5 years
    }
}


