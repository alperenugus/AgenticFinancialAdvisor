package com.agent.financialadvisor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks tool executions for a session to send via WebSocket
 */
@Component
public class ToolExecutionTracker {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionTracker.class);
    private final Map<String, ToolCallInfo> activeToolCalls = new ConcurrentHashMap<>();

    public void startToolCall(String sessionId, String toolName, Map<String, Object> parameters) {
        String callId = generateCallId();
        ToolCallInfo info = new ToolCallInfo(callId, toolName, parameters, System.currentTimeMillis());
        activeToolCalls.put(callId, info);
        log.debug("Started tool call: {} for session {}", toolName, sessionId);
    }

    public void completeToolCall(String callId, Object result) {
        ToolCallInfo info = activeToolCalls.remove(callId);
        if (info != null) {
            info.setResult(result);
            info.setCompletedAt(System.currentTimeMillis());
            log.debug("Completed tool call: {} in {}ms", info.getToolName(), info.getDuration());
        }
    }

    public void failToolCall(String callId, String error) {
        ToolCallInfo info = activeToolCalls.remove(callId);
        if (info != null) {
            info.setError(error);
            info.setCompletedAt(System.currentTimeMillis());
            log.debug("Failed tool call: {} - {}", info.getToolName(), error);
        }
    }

    private String generateCallId() {
        return "tool-" + System.currentTimeMillis() + "-" + 
               Long.toString(System.currentTimeMillis(), 36).substring(2);
    }

    public static class ToolCallInfo {
        private String callId;
        private String toolName;
        private Map<String, Object> parameters;
        private Object result;
        private String error;
        private long startedAt;
        private Long completedAt;

        public ToolCallInfo(String callId, String toolName, Map<String, Object> parameters, long startedAt) {
            this.callId = callId;
            this.toolName = toolName;
            this.parameters = parameters;
            this.startedAt = startedAt;
        }

        public long getDuration() {
            return completedAt != null ? completedAt - startedAt : System.currentTimeMillis() - startedAt;
        }

        public String getHumanFriendlyDescription() {
            StringBuilder desc = new StringBuilder();
            desc.append(formatToolName(toolName));
            
            if (parameters != null && !parameters.isEmpty()) {
                desc.append(" with parameters: ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    if (!first) desc.append(", ");
                    desc.append(entry.getKey()).append("=").append(formatValue(entry.getValue()));
                    first = false;
                }
            }
            
            if (error != null) {
                desc.append(" (failed: ").append(error).append(")");
            } else if (result != null) {
                desc.append(" (completed)");
            } else {
                desc.append(" (in progress)");
            }
            
            return desc.toString();
        }

        private String formatToolName(String toolName) {
            // Convert camelCase to human-readable
            return toolName
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .toLowerCase()
                .replaceAll("^.", String.valueOf(toolName.charAt(0)).toUpperCase());
        }

        private String formatValue(Object value) {
            if (value == null) return "null";
            String str = value.toString();
            if (str.length() > 50) {
                return str.substring(0, 47) + "...";
            }
            return str;
        }

        // Getters and setters
        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getStartedAt() { return startedAt; }
        public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
        public Long getCompletedAt() { return completedAt; }
        public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    }
}

