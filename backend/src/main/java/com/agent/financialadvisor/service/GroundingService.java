package com.agent.financialadvisor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic numeric-grounding check: every figure in a user-facing response must be present
 * in the tool/source data that produced it. This is the hard backstop behind the evaluator
 * prompt's "never fabricate data" instruction — an LLM rule alone cannot be trusted in a
 * financial product.
 *
 * A response number counts as grounded when any number extracted from the sources matches it
 * within a small tolerance (handles 93.7 vs 93.70 style rounding). Small counting integers and
 * calendar years are whitelisted to avoid false positives on prose like "your 2 holdings".
 */
@Service
public class GroundingService {

    private static final Logger log = LoggerFactory.getLogger(GroundingService.class);

    /** Matches 290.55 / 2,905.50 / 1405.5 / 93.70 etc. (currency/percent symbols handled around it). */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");

    private static final double RELATIVE_TOLERANCE = 0.005;  // 0.5%
    private static final double ABSOLUTE_TOLERANCE = 0.011;  // covers 2-dp rounding

    /** Integers with |value| <= this are treated as counts/prose and never flagged. */
    private static final int SMALL_INT_WHITELIST = 12;

    /**
     * Returns the distinct numeric tokens in {@code response} that cannot be matched to any
     * number present in {@code sources}. Empty list = fully grounded.
     */
    public List<String> findUngroundedNumbers(String response, Collection<String> sources) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        List<Double> sourceNumbers = new ArrayList<>();
        if (sources != null) {
            for (String source : sources) {
                sourceNumbers.addAll(extractNumbers(source));
            }
        }

        Set<String> offenders = new LinkedHashSet<>();
        Matcher m = NUMBER_PATTERN.matcher(response);
        while (m.find()) {
            String token = m.group();
            Double value = parse(token);
            if (value == null || isWhitelisted(token, value)) {
                continue;
            }
            if (!isGrounded(value, sourceNumbers)) {
                offenders.add(token);
            }
        }
        if (!offenders.isEmpty()) {
            log.warn("🚨 [GROUNDING] Ungrounded figures in response: {}", offenders);
        }
        return new ArrayList<>(offenders);
    }

    /** True when the response contains no figures missing from the sources. */
    public boolean isGrounded(String response, Collection<String> sources) {
        return findUngroundedNumbers(response, sources).isEmpty();
    }

    private List<Double> extractNumbers(String text) {
        List<Double> numbers = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return numbers;
        }
        Matcher m = NUMBER_PATTERN.matcher(text);
        while (m.find()) {
            Double value = parse(m.group());
            if (value != null) {
                numbers.add(value);
            }
        }
        return numbers;
    }

    private Double parse(String token) {
        try {
            return Double.parseDouble(token.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isWhitelisted(String token, double value) {
        boolean isInteger = !token.contains(".");
        if (isInteger && Math.abs(value) <= SMALL_INT_WHITELIST) {
            return true; // counts in prose: "2 holdings", "3 steps"
        }
        if (isInteger && value >= 1900 && value <= 2100) {
            return true; // calendar years
        }
        return false;
    }

    private boolean isGrounded(double value, List<Double> sourceNumbers) {
        for (double source : sourceNumbers) {
            double absDiff = Math.abs(source - value);
            if (absDiff <= ABSOLUTE_TOLERANCE) {
                return true;
            }
            double magnitude = Math.max(Math.abs(source), Math.abs(value));
            if (magnitude > 0 && absDiff / magnitude <= RELATIVE_TOLERANCE) {
                return true;
            }
        }
        return false;
    }
}
