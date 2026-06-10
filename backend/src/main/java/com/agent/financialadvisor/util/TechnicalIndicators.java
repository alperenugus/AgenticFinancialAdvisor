package com.agent.financialadvisor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure technical-indicator math over a daily close series (oldest → newest).
 * Standard formulas only — no heuristics — so the advisor's "technical analysis"
 * reports real, reproducible numbers instead of pseudo-analysis.
 */
public final class TechnicalIndicators {

    private TechnicalIndicators() {
    }

    /** Simple moving average of the last {@code period} closes. Null if not enough data. */
    public static BigDecimal sma(double[] closes, int period) {
        if (closes == null || period <= 0 || closes.length < period) {
            return null;
        }
        double sum = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            sum += closes[i];
        }
        return round2(sum / period);
    }

    /**
     * Relative Strength Index (Wilder's smoothing) over {@code period} (conventionally 14).
     * Null if fewer than period+1 closes.
     */
    public static BigDecimal rsi(double[] closes, int period) {
        if (closes == null || period <= 0 || closes.length < period + 1) {
            return null;
        }
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double change = closes[i] - closes[i - 1];
            if (change > 0) avgGain += change; else avgLoss -= change;
        }
        avgGain /= period;
        avgLoss /= period;
        for (int i = period + 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }
        if (avgLoss == 0) {
            return round2(100.0);
        }
        double rs = avgGain / avgLoss;
        return round2(100.0 - (100.0 / (1.0 + rs)));
    }

    /**
     * Annualized realized volatility (%) from daily log returns over the last
     * {@code lookbackDays} closes (sample stdev × √252). Null if not enough data.
     */
    public static BigDecimal annualizedVolatilityPercent(double[] closes, int lookbackDays) {
        if (closes == null || lookbackDays < 2 || closes.length < lookbackDays + 1) {
            return null;
        }
        double[] returns = new double[lookbackDays];
        int start = closes.length - lookbackDays;
        for (int i = 0; i < lookbackDays; i++) {
            double prev = closes[start + i - 1];
            double curr = closes[start + i];
            if (prev <= 0 || curr <= 0) {
                return null;
            }
            returns[i] = Math.log(curr / prev);
        }
        double mean = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;
        double variance = 0;
        for (double r : returns) variance += (r - mean) * (r - mean);
        variance /= (returns.length - 1);
        return round2(Math.sqrt(variance) * Math.sqrt(252.0) * 100.0);
    }

    /** Percent return between the close {@code tradingDaysAgo} back and the latest close. Null if not enough data. */
    public static BigDecimal periodReturnPercent(double[] closes, int tradingDaysAgo) {
        if (closes == null || tradingDaysAgo <= 0 || closes.length <= tradingDaysAgo) {
            return null;
        }
        double past = closes[closes.length - 1 - tradingDaysAgo];
        double latest = closes[closes.length - 1];
        if (past <= 0) {
            return null;
        }
        return round2((latest - past) / past * 100.0);
    }

    /** Highest value in the series. Null on empty input. */
    public static BigDecimal high(double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double max = values[0];
        for (double v : values) max = Math.max(max, v);
        return round2(max);
    }

    /** Lowest positive value in the series. Null on empty input. */
    public static BigDecimal low(double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double min = Double.MAX_VALUE;
        for (double v : values) {
            if (v > 0) min = Math.min(min, v);
        }
        return min == Double.MAX_VALUE ? null : round2(min);
    }

    private static BigDecimal round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
