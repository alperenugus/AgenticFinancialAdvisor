package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LlmTickerResolver implements TickerResolver {

    private static final Logger log = LoggerFactory.getLogger(LlmTickerResolver.class);

    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final SymbolResolutionPlannerAgent plannerAgent;
    private final SymbolSelectionAgent selectionAgent;
    private final SymbolSelectionEvaluatorAgent evaluatorAgent;
    private final SymbolSelectionAuditorAgent auditorAgent;

    @Autowired
    public LlmTickerResolver(
            ChatLanguageModel orchestratorModel,
            @Qualifier("agentChatLanguageModel") ChatLanguageModel selectorModel,
            ObjectMapper objectMapper,
            @Value("${market-data.symbol-resolution.max-attempts:2}") int maxAttempts
    ) {
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.plannerAgent = AiServices.builder(SymbolResolutionPlannerAgent.class)
                .chatLanguageModel(orchestratorModel)
                .build();
        this.selectionAgent = AiServices.builder(SymbolSelectionAgent.class)
                .chatLanguageModel(selectorModel)
                .build();
        this.evaluatorAgent = AiServices.builder(SymbolSelectionEvaluatorAgent.class)
                .chatLanguageModel(orchestratorModel)
                .build();
        this.auditorAgent = AiServices.builder(SymbolSelectionAuditorAgent.class)
                .chatLanguageModel(selectorModel)
                .build();
    }

    LlmTickerResolver(
            ObjectMapper objectMapper,
            int maxAttempts,
            SymbolResolutionPlannerAgent plannerAgent,
            SymbolSelectionAgent selectionAgent,
            SymbolSelectionEvaluatorAgent evaluatorAgent,
            SymbolSelectionAuditorAgent auditorAgent
    ) {
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.plannerAgent = plannerAgent;
        this.selectionAgent = selectionAgent;
        this.evaluatorAgent = evaluatorAgent;
        this.auditorAgent = auditorAgent;
    }

    @Override
    public Decision resolve(String userInput, List<Candidate> candidates) {
        if (userInput == null || userInput.isBlank() || candidates == null || candidates.isEmpty()) {
            return new Decision(null, false, "No candidates available for ticker resolution");
        }

        String planningNotes = buildPlanningNotes(userInput, candidates);
        String feedback = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String selectedSymbol = selectSymbol(userInput, candidates, planningNotes, feedback);
            if (selectedSymbol == null || selectedSymbol.isBlank()) {
                feedback = "No symbol selected. Choose the best match from candidates or return null.";
                continue;
            }

            EvaluationVerdict evaluatorVerdict = evaluateSelection(userInput, candidates, selectedSymbol, planningNotes);
            if (!evaluatorVerdict.pass()) {
                feedback = evaluatorVerdict.reason();
                log.info("Ticker evaluator rejected symbol={} for input='{}' on attempt {}/{}: {}",
                        selectedSymbol, userInput, attempt, maxAttempts, feedback);
                continue;
            }

            EvaluationVerdict auditorVerdict = auditSelection(
                    userInput,
                    candidates,
                    selectedSymbol,
                    planningNotes,
                    evaluatorVerdict.reason()
            );
            if (auditorVerdict.pass()) {
                String reason = "Evaluator: " + evaluatorVerdict.reason() + " | Auditor: " + auditorVerdict.reason();
                return new Decision(selectedSymbol, true, reason);
            }

            feedback = auditorVerdict.reason();
            log.info("Ticker auditor rejected symbol={} for input='{}' on attempt {}/{}: {}",
                    selectedSymbol, userInput, attempt, maxAttempts, feedback);
        }

        return new Decision(null, false, "LLM resolver could not produce a confident ticker selection");
    }

    private String buildPlanningNotes(String userInput, List<Candidate> candidates) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("candidates", candidates);
            payload.put("instruction", "Create concise disambiguation notes for ticker selection.");
            String raw = plannerAgent.plan(objectMapper.writeValueAsString(payload));
            if (raw == null || raw.isBlank()) {
                return "{\"objective\":\"Select correct ticker\",\"checks\":[\"identity match\",\"avoid lookalikes\"]}";
            }
            return raw.substring(0, Math.min(raw.length(), 800));
        } catch (Exception e) {
            log.warn("Ticker planning failed for '{}': {}", userInput, e.getMessage());
            return "{\"objective\":\"Select correct ticker\",\"checks\":[\"identity match\",\"avoid lookalikes\"]}";
        }
    }

    private String selectSymbol(
            String userInput,
            List<Candidate> candidates,
            String planningNotes,
            String feedback
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("candidates", candidates);
            payload.put("planningNotes", planningNotes);
            if (feedback != null && !feedback.isBlank()) {
                payload.put("feedback", feedback);
            }
            payload.put("instruction", "Choose one candidate symbol or null if no confident match. Use only provided candidates.");
            String raw = selectionAgent.select(objectMapper.writeValueAsString(payload));
            return parseSelectedSymbol(raw);
        } catch (Exception e) {
            log.warn("Ticker selection failed for '{}': {}", userInput, e.getMessage());
            return null;
        }
    }

    private EvaluationVerdict evaluateSelection(
            String userInput,
            List<Candidate> candidates,
            String selectedSymbol,
            String planningNotes
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("selectedSymbol", selectedSymbol);
            payload.put("candidates", candidates);
            payload.put("planningNotes", planningNotes);
            payload.put("criteria", "PASS if selectedSymbol best matches userInput identity and is from candidates; FAIL otherwise.");
            String raw = evaluatorAgent.evaluate(objectMapper.writeValueAsString(payload));
            return parseEvaluationVerdict(raw);
        } catch (Exception e) {
            log.warn("Ticker evaluation failed for '{}': {}", userInput, e.getMessage());
            return EvaluationVerdict.pass("Evaluator unavailable");
        }
    }

    private EvaluationVerdict auditSelection(
            String userInput,
            List<Candidate> candidates,
            String selectedSymbol,
            String planningNotes,
            String evaluatorReason
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userInput", userInput);
            payload.put("selectedSymbol", selectedSymbol);
            payload.put("candidates", candidates);
            payload.put("planningNotes", planningNotes);
            payload.put("evaluatorReason", evaluatorReason);
            payload.put("criteria", "Independent audit: PASS only if selection is safe and identity-correct.");
            String raw = auditorAgent.audit(objectMapper.writeValueAsString(payload));
            return parseEvaluationVerdict(raw);
        } catch (Exception e) {
            log.warn("Ticker auditing failed for '{}': {}", userInput, e.getMessage());
            return EvaluationVerdict.pass("Auditor unavailable");
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
            return null;
        }
    }

    private EvaluationVerdict parseEvaluationVerdict(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return EvaluationVerdict.fail("Evaluator returned empty output");
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
        } catch (Exception e) {
            return EvaluationVerdict.fail("Evaluator returned non-JSON output: " + e.getMessage());
        }
        return EvaluationVerdict.fail("Evaluator verdict missing PASS/FAIL");
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

    interface SymbolResolutionPlannerAgent {
        @SystemMessage("You are a planning agent for stock symbol resolution.\n" +
                "Analyze user input and candidate list to produce disambiguation guidance.\n" +
                "Respond in concise JSON with keys objective, risks, checks.")
        String plan(@UserMessage String payloadJson);
    }

    interface SymbolSelectionAgent {
        @SystemMessage("You are a stock ticker selection agent.\n" +
                "Given user input and a list of live candidates, choose the single best symbol.\n" +
                "Rules:\n" +
                "- Choose ONLY from provided candidates.\n" +
                "- If no confident match exists, return null symbol.\n" +
                "- Never invent symbols.\n" +
                "Respond ONLY as JSON: {\"symbol\":\"<candidate-or-null>\",\"reason\":\"...\",\"confidence\":\"HIGH|MEDIUM|LOW\"}")
        String select(@UserMessage String payloadJson);
    }

    interface SymbolSelectionEvaluatorAgent {
        @SystemMessage("You are an evaluator for ticker selection.\n" +
                "Given user input, candidates, and selectedSymbol, verify selection quality.\n" +
                "PASS only if identity match is strong and selectedSymbol exists in candidate list.\n" +
                "FAIL if there is a likely company/ticker mismatch.\n" +
                "Respond ONLY as JSON: {\"verdict\":\"PASS|FAIL\",\"reason\":\"...\"}")
        String evaluate(@UserMessage String payloadJson);
    }

    interface SymbolSelectionAuditorAgent {
        @SystemMessage("You are an independent auditor for ticker selection.\n" +
                "Use user input, candidate list, selectedSymbol, and evaluator reason.\n" +
                "Perform an independent second check.\n" +
                "PASS only if the symbol is safe to use and identity-consistent.\n" +
                "Respond ONLY as JSON: {\"verdict\":\"PASS|FAIL\",\"reason\":\"...\"}")
        String audit(@UserMessage String payloadJson);
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
