package com.agent.financialadvisor.service.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Parses evaluator model output into a deterministic decision object.
 * This keeps orchestration retry logic stable even when evaluator formatting varies.
 */
public class EvaluatorDecisionParser {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorDecisionParser.class);
    private final ObjectMapper objectMapper;

    public EvaluatorDecisionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationDecision parse(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return EvaluationDecision.pass("Evaluator returned empty output");
        }

        String cleaned = stripCodeFences(rawOutput.trim());
        String jsonSegment = extractJsonObject(cleaned);
        if (jsonSegment != null) {
            try {
                JsonNode root = objectMapper.readTree(jsonSegment);
                String verdict = root.path("verdict").asText("").trim().toUpperCase(Locale.ROOT);
                String reason = root.path("reason").asText("No reason provided");
                String retryInstruction = root.path("retryInstruction").asText(reason);
                if ("FAIL".equals(verdict)) {
                    return EvaluationDecision.fail(reason, retryInstruction);
                }
                if ("PASS".equals(verdict)) {
                    return EvaluationDecision.pass(reason);
                }
            } catch (Exception ignored) {
                log.warn("Could not parse evaluator JSON output: {}", rawOutput);
            }
        }

        String normalized = cleaned.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("FAIL")) {
            return EvaluationDecision.fail("Evaluator indicated failure", cleaned);
        }
        if (normalized.startsWith("PASS")) {
            return EvaluationDecision.pass("Evaluator indicated pass");
        }

        return EvaluationDecision.pass("Evaluator verdict unclear");
    }

    private String stripCodeFences(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine >= 0) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    public static final class EvaluationDecision {
        private final boolean pass;
        private final String reason;
        private final String retryInstruction;

        private EvaluationDecision(boolean pass, String reason, String retryInstruction) {
            this.pass = pass;
            this.reason = reason;
            this.retryInstruction = retryInstruction;
        }

        public static EvaluationDecision pass(String reason) {
            return new EvaluationDecision(true, reason, "");
        }

        public static EvaluationDecision fail(String reason, String retryInstruction) {
            return new EvaluationDecision(false, reason, retryInstruction);
        }

        public boolean isPass() {
            return pass;
        }

        public String getReason() {
            return reason;
        }

        public String getRetryInstruction() {
            return retryInstruction;
        }
    }
}
