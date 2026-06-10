package com.agent.financialadvisor.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorsTest {

    @Test
    void sma_averagesLastPeriodCloses() {
        double[] closes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertThat(TechnicalIndicators.sma(closes, 5)).isEqualByComparingTo(new BigDecimal("8.00")); // (6+7+8+9+10)/5
        assertThat(TechnicalIndicators.sma(closes, 10)).isEqualByComparingTo(new BigDecimal("5.50"));
    }

    @Test
    void sma_returnsNullWhenInsufficientData() {
        assertThat(TechnicalIndicators.sma(new double[]{1, 2, 3}, 5)).isNull();
        assertThat(TechnicalIndicators.sma(null, 5)).isNull();
    }

    @Test
    void rsi_is100ForPureUptrend_andLowForPureDowntrend() {
        double[] rising = IntStream.rangeClosed(1, 30).mapToDouble(i -> 100.0 + i).toArray();
        assertThat(TechnicalIndicators.rsi(rising, 14)).isEqualByComparingTo(new BigDecimal("100.00"));

        double[] falling = IntStream.rangeClosed(1, 30).mapToDouble(i -> 200.0 - i).toArray();
        assertThat(TechnicalIndicators.rsi(falling, 14).doubleValue()).isLessThan(5.0);
    }

    @Test
    void rsi_isNeutralForAlternatingEqualMoves() {
        // +1/-1 alternating: average gain == average loss → RSI ≈ 50
        double[] closes = new double[40];
        closes[0] = 100;
        for (int i = 1; i < closes.length; i++) {
            closes[i] = closes[i - 1] + (i % 2 == 0 ? -1 : 1);
        }
        double rsi = TechnicalIndicators.rsi(closes, 14).doubleValue();
        assertThat(rsi).isBetween(40.0, 60.0);
    }

    @Test
    void rsi_returnsNullWhenInsufficientData() {
        assertThat(TechnicalIndicators.rsi(new double[]{1, 2, 3}, 14)).isNull();
    }

    @Test
    void volatility_isZeroForConstantSeries_andPositiveForNoisySeries() {
        double[] flat = new double[40];
        java.util.Arrays.fill(flat, 100.0);
        assertThat(TechnicalIndicators.annualizedVolatilityPercent(flat, 30))
                .isEqualByComparingTo(new BigDecimal("0.00"));

        double[] noisy = new double[40];
        noisy[0] = 100;
        for (int i = 1; i < noisy.length; i++) {
            noisy[i] = noisy[i - 1] * (i % 2 == 0 ? 1.02 : 0.985);
        }
        assertThat(TechnicalIndicators.annualizedVolatilityPercent(noisy, 30).doubleValue()).isGreaterThan(10.0);
    }

    @Test
    void periodReturn_computesPercentChange() {
        double[] closes = {100, 105, 110, 121};
        assertThat(TechnicalIndicators.periodReturnPercent(closes, 3))
                .isEqualByComparingTo(new BigDecimal("21.00")); // 100 → 121
        assertThat(TechnicalIndicators.periodReturnPercent(closes, 1))
                .isEqualByComparingTo(new BigDecimal("10.00")); // 110 → 121
    }

    @Test
    void highLow_findExtremes() {
        double[] values = {50.5, 75.25, 42.1, 60.0};
        assertThat(TechnicalIndicators.high(values)).isEqualByComparingTo(new BigDecimal("75.25"));
        assertThat(TechnicalIndicators.low(values)).isEqualByComparingTo(new BigDecimal("42.10"));
    }
}
