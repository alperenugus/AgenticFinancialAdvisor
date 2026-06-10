package com.agent.financialadvisor.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @OneToMany(mappedBy = "portfolioId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockHolding> holdings = new ArrayList<>();

    @Column(name = "total_value", precision = 19, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "total_gain_loss", precision = 19, scale = 2)
    private BigDecimal totalGainLoss = BigDecimal.ZERO;

    @Column(name = "total_gain_loss_percent", precision = 19, scale = 4)
    private BigDecimal totalGainLossPercent = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        recalculateTotals();
    }

    /**
     * Recompute totals directly from each holding's raw fields (currentPrice/averagePrice/quantity)
     * rather than from the holdings' derived {@code value}/{@code gainLoss} columns.
     *
     * Why: JPA does NOT guarantee that a child's @PreUpdate (which sets StockHolding.value) fires
     * before the parent's @PreUpdate. The previous implementation summed h.getValue(), so when a
     * portfolio was saved after a price refresh, the children's value columns could still be null at
     * the moment this ran — persisting a total of 0.00 even though every holding had a real value.
     * Computing from currentPrice*quantity here is order-independent and always correct.
     *
     * PUBLIC and called EXPLICITLY by the controllers/agents after refreshing holding prices: when
     * only child holdings change, Hibernate doesn't mark the parent dirty, so this callback alone
     * would not fire and the total would stay stale (the "$0 total beside live holdings" bug).
     * Calling it explicitly both computes the total AND dirties the parent so it persists.
     */
    public void recalculateTotals() {
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        if (holdings != null) {
            for (StockHolding h : holdings) {
                if (h == null || h.getQuantity() == null) {
                    continue;
                }
                BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
                BigDecimal current = h.getCurrentPrice();
                BigDecimal avg = h.getAveragePrice();

                // Use the live price when we have it; fall back to cost basis so a single failed
                // price refresh never silently zeroes out a holding's contribution to the total.
                BigDecimal effectivePrice = (current != null && current.signum() > 0) ? current : avg;
                if (effectivePrice != null) {
                    value = value.add(effectivePrice.multiply(qty));
                }
                if (current != null && current.signum() > 0 && avg != null) {
                    gain = gain.add(current.subtract(avg).multiply(qty));
                }
                if (avg != null) {
                    cost = cost.add(avg.multiply(qty));
                }
            }
        }

        totalValue = value;
        totalGainLoss = gain;
        totalGainLossPercent = cost.compareTo(BigDecimal.ZERO) > 0
                ? gain.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<StockHolding> getHoldings() { return holdings; }
    public void setHoldings(List<StockHolding> holdings) { this.holdings = holdings; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getTotalGainLoss() { return totalGainLoss; }
    public void setTotalGainLoss(BigDecimal totalGainLoss) { this.totalGainLoss = totalGainLoss; }
    public BigDecimal getTotalGainLossPercent() { return totalGainLossPercent; }
    public void setTotalGainLossPercent(BigDecimal totalGainLossPercent) { this.totalGainLossPercent = totalGainLossPercent; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}


