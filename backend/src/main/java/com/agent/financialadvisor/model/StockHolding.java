package com.agent.financialadvisor.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_holdings")
public class StockHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(nullable = false, length = 10)
    private String symbol; // e.g., "AAPL", "GOOGL"

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal averagePrice;

    @Column(name = "current_price", precision = 19, scale = 2)
    private BigDecimal currentPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal value; // quantity * currentPrice

    @Column(name = "gain_loss", precision = 19, scale = 2)
    private BigDecimal gainLoss; // (currentPrice - averagePrice) * quantity

    @Column(name = "gain_loss_percent", precision = 19, scale = 4)
    private BigDecimal gainLossPercent; // ((currentPrice - averagePrice) / averagePrice) * 100

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        if (quantity == null) {
            return;
        }
        BigDecimal qty = BigDecimal.valueOf(quantity);
        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            value = currentPrice.multiply(qty);
            if (averagePrice != null) {
                BigDecimal priceDiff = currentPrice.subtract(averagePrice);
                gainLoss = priceDiff.multiply(qty);
                if (averagePrice.compareTo(BigDecimal.ZERO) > 0) {
                    gainLossPercent = priceDiff.divide(averagePrice, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }
        } else if (averagePrice != null) {
            // No live price (refresh failed/unavailable): value the holding at cost so it isn't
            // silently zeroed. Gain/loss is left unset because it can't be computed without a quote.
            value = averagePrice.multiply(qty);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
    public BigDecimal getGainLossPercent() { return gainLossPercent; }
    public void setGainLossPercent(BigDecimal gainLossPercent) { this.gainLossPercent = gainLossPercent; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}


