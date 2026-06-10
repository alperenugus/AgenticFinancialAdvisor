package com.agent.financialadvisor.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTest {

    private StockHolding holding(String symbol, int qty, String avg, String current) {
        StockHolding h = new StockHolding();
        h.setSymbol(symbol);
        h.setQuantity(qty);
        h.setAveragePrice(new BigDecimal(avg));
        if (current != null) {
            h.setCurrentPrice(new BigDecimal(current));
        }
        // Deliberately DO NOT set value/gainLoss — this reproduces the state in which the parent's
        // @PreUpdate (calculateTotals) ran before the children's @PreUpdate populated those fields.
        return h;
    }

    private void recalc(Portfolio p) {
        p.recalculateTotals();
    }

    @Test
    void totals_areComputedFromCurrentPriceEvenWhenHoldingValueFieldIsNull() {
        // The $0.00 bug: holdings have live currentPrice but their derived value column is still null.
        Portfolio p = new Portfolio();
        p.setHoldings(List.of(
                holding("AMD", 157, "212.00", "459.81"),
                holding("NVDA", 35, "180.00", "203.16")
        ));

        recalc(p);

        // 157*459.81 + 35*203.16 = 72190.17 + 7110.60 = 79300.77
        assertThat(p.getTotalValue()).isEqualByComparingTo(new BigDecimal("79300.77"));
        assertThat(p.getTotalValue().signum()).isPositive(); // never $0.00 with priced holdings
    }

    @Test
    void gainLoss_andPercent_areCorrect() {
        Portfolio p = new Portfolio();
        // 10 shares, cost 150, now 290.55 → value 2905.50, gain (290.55-150)*10 = 1405.50, cost 1500 → 93.70%
        p.setHoldings(List.of(holding("AAPL", 10, "150.00", "290.55")));

        recalc(p);

        assertThat(p.getTotalValue()).isEqualByComparingTo(new BigDecimal("2905.50"));
        assertThat(p.getTotalGainLoss()).isEqualByComparingTo(new BigDecimal("1405.50"));
        assertThat(p.getTotalGainLossPercent()).isEqualByComparingTo(new BigDecimal("93.7000"));
    }

    @Test
    void missingLivePrice_fallsBackToCostBasis_notZero() {
        Portfolio p = new Portfolio();
        // No current price (refresh failed): valued at cost (10 * 150 = 1500), no gain computed.
        p.setHoldings(List.of(holding("AAPL", 10, "150.00", null)));

        recalc(p);

        assertThat(p.getTotalValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(p.getTotalGainLoss()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void emptyPortfolio_isZero() {
        Portfolio p = new Portfolio();
        recalc(p);
        assertThat(p.getTotalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getTotalGainLossPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void stockHolding_onUpdate_valuesAtCostWhenNoCurrentPrice() {
        StockHolding h = holding("AAPL", 10, "150.00", null);
        ReflectionTestUtils.invokeMethod(h, "onUpdate");
        assertThat(h.getValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }
}
