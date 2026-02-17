package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LlmTickerResolver implements TickerResolver {

    private static final Logger log = LoggerFactory.getLogger(LlmTickerResolver.class);

    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final SymbolSelectionAgent selectionAgent;
    private final SymbolSelectionEvaluatorAgent evaluatorAgent;

    public LlmTickerResolver(
            @Qualifier("agentChatLanguageModel") ChatLanguageModel chatLanguageModel,
            ObjectMapper objectMapper,
            @Value("${market-data.symbol-resolution.max-attempts:2}") int maxAttempts
    ) {
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.selectionAgent = AiServices.builder(SymbolSelectionAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
        this.evaluatorAgent = AiServices.builder(SymbolSelectionEvaluatorAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    @Override
    public Decision resolve(String userInput, List<Candidate> candidates) {
        if (userInput == null || userInput.isBlank() || candidates == null || candidates.isEmpty()) {
            return new Decision(null, false, "No candidates available for ticker resolution");
        }

        String feedback = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String selectedSymbol = selectSymbol(userInput, candidates, feedback);
            if (selectedSymbol == null || selectedSymbol.isBlank()) {
                feedback = "No symbol selected. Choose the best match from candidates or return null.";
                continue;
            }

            if (!containsCandidate(candidates, selectedSymbol)) {
                feedback = "Selected symbol must be one of the provided candidates.";
                continue;
            }

            EvaluationVerdict verdict = evaluateSelection(userInput, candidates, selectedSymbol);
            if (verdict.pass()) {
                return new Decision(selectedSymbol, true, verdict.reason());
            }

            feedback = verdict.reason();
            log.info("Ticker evaluator rejected symbol={} for input='{}' on attempt {}/{}: {}",
                    selectedSymbol, userInput, attempt, maxAttempts, feedback);
        }

        return new Decision(null, false, "LLM resolver could not produce a confident ticker selection");
    }

    private String selectSymbol(String userInput, List<Candidate> candidates, String feedback) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("candidates", candidates);
            if (feedback != null && !feedback.isBlank()) {
                payload.put("feedback", feedback);
            }
            payload.put("instruction", "Choose one candidate symbol or null if no confident match.");
            String raw = selectionAgent.select(objectMapper.writeValueAsString(payload));
            return parseSelectedSymbol(raw);
        } catch (Exception e) {
            log.warn("Ticker selection failed for '{}': {}", userInput, e.getMessage());
            return null;
        }
    }

    private EvaluationVerdict evaluateSelection(String userInput, List<Candidate> candidates, String selectedSymbol) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("selectedSymbol", selectedSymbol);
            payload.put("candidates", candidates);
            payload.put("criteria", "PASS if selectedSymbol best matches userInput identity; FAIL otherwise.");
            String raw = evaluatorAgent.evaluate(objectMapper.writeValueAsString(payload));
            return parseEvaluationVerdict(raw);
        } catch (Exception e) {
            log.warn("Ticker evaluation failed for '{}': {}", userInput, e.getMessage());
            return EvaluationVerdict.pass("Evaluator unavailable");
        }
    }

    private String parseSelectedSymbol(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return null;
        }

        String cleaned = stripCodeFences(rawOutput.trim());
        try {
            JsonNode root = objectMapper.readTree(extractJsonOrRaw(cleaned));
            String symbol = root.path("symbol").asText("").trim().toUpperCase(Locale.ROOT);
            if (symbol.isBlank() || "NULL".equals(symbol)) {
                return null;
            }
            return symbol;
        } catch (Exception ignored) {
            String fallback = cleaned.toUpperCase(Locale.ROOT)
                    .replaceAll("[^A-Z0-9.\\-]", "")
                    .trim();
            return fallback.isBlank() ? null : fallback;
        }
    }

    private EvaluationVerdict parseEvaluationVerdict(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return EvaluationVerdict.pass("Evaluator returned empty output");
        }

        String cleaned = stripCodeFences(rawOutput.trim());
        String jsonOrRaw = extractJsonOrRaw(cleaned);
        try {
            JsonNode root = objectMapper.readTree(jsonOrRaw);
            String verdict = root.path("verdict").asText("").trim().toUpperCase(Locale.ROOT);
            String reason = root.path("reason").asText("No reason provided");
            if ("FAIL".equals(verdict)) {
                return EvaluationVerdict.fail(reason);
            }
            if ("PASS".equals(verdict)) {
                return EvaluationVerdict.pass(reason);
            }
        } catch (Exception ignored) {
            // Fallback to text parsing
        }

        String normalized = cleaned.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("FAIL")) {
            return EvaluationVerdict.fail(cleaned);
        }
        if (normalized.startsWith("PASS")) {
            return EvaluationVerdict.pass(cleaned);
        }
        return EvaluationVerdict.pass("Evaluator verdict unclear");
    }

    private boolean containsCandidate(List<Candidate> candidates, String symbol) {
        Set<String> symbols = candidates.stream()
                .map(c -> c.symbol() == null ? "" : c.symbol().trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        return symbols.contains(symbol.toUpperCase(Locale.ROOT));
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String extractJsonOrRaw(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private interface SymbolSelectionAgent {
        @SystemMessage("You are a stock ticker selection agent.\n" +
                "Given user input and a list of live candidates, choose the single best symbol.\n" +
                "Rules:\n" +
                "- Choose ONLY from provided candidates.\n" +
                "- If no confident match exists, return null symbol.\n" +
                "- Never invent symbols.\n" +
                "Respond ONLY as JSON: {\"symbol\":\"<candidate-or-null>\",\"reason\":\"...\",\"confidence\":\"HIGH|MEDIUM|LOW\"}")
        String select(@UserMessage String payloadJson);
    }

    private interface SymbolSelectionEvaluatorAgent {
        @SystemMessage("You are an evaluator for ticker selection.\n" +
                "Given user input, candidates, and selectedSymbol, verify selection quality.\n" +
                "PASS only if identity match is strong.\n" +
                "FAIL if there is a likely company/ticker mismatch.\n" +
                "Respond ONLY as JSON: {\"verdict\":\"PASS|FAIL\",\"reason\":\"...\"}")
        String evaluate(@UserMessage String payloadJson);
    }

    private record EvaluationVerdict(boolean pass, String reason) {
        static EvaluationVerdict pass(String reason) {
            return new EvaluationVerdict(true, reason);
        }

        static EvaluationVerdict fail(String reason) {
            return new EvaluationVerdict(false, reason);
        }
    }
}
