package com.agent.financialadvisor.aspect;

import com.agent.financialadvisor.service.WebSocketService;
import dev.langchain4j.agent.tool.Tool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AOP Aspect to intercept tool calls and send updates via WebSocket
 * This allows users to see what tools the agent is using in real-time.
 *
 * It also captures the RAW result of every @Tool call per session. This matters for grounding:
 * sub-agents return an LLM paraphrase of their tool data, so without this capture the evaluator
 * (and the deterministic grounding check) would only ever see second-hand numbers. The orchestrator
 * drains these raw results and feeds them to the evaluator + GroundingService as ground truth.
 */
@Aspect
@Component
@Order(1)
public class ToolCallAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAspect.class);
    private static final int MAX_RAW_RESULTS_PER_SESSION = 30;
    private static final int MAX_RAW_RESULT_LENGTH = 4000;

    private final WebSocketService webSocketService;

    // Thread-local storage for session ID (set by OrchestratorService)
    // Using InheritableThreadLocal to propagate to child threads (CompletableFuture)
    private static final InheritableThreadLocal<String> sessionIdHolder = new InheritableThreadLocal<>();

    // Raw tool outputs per session (cross-thread: steps run on a pool). Bounded; cleared by the
    // orchestrator before each execution and on session clear.
    private static final Map<String, List<String>> sessionToolResults = new ConcurrentHashMap<>();

    @Autowired
    public ToolCallAspect(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    public static void setSessionId(String sessionId) {
        sessionIdHolder.set(sessionId);
    }

    public static void clearSessionId() {
        sessionIdHolder.remove();
    }

    public static String getSessionId() {
        return sessionIdHolder.get();
    }

    /** Remove and return all raw tool results captured for this session so far. */
    public static List<String> drainToolResults(String sessionId) {
        List<String> results = sessionToolResults.remove(sessionId);
        return results != null ? new ArrayList<>(results) : new ArrayList<>();
    }

    /** Drop any captured raw tool results for this session (used on session clear / before runs). */
    public static void clearToolResults(String sessionId) {
        sessionToolResults.remove(sessionId);
    }

    private static void recordToolResult(String sessionId, String toolMethod, Object result) {
        if (sessionId == null || result == null) {
            return;
        }
        List<String> results = sessionToolResults.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        if (results.size() >= MAX_RAW_RESULTS_PER_SESSION) {
            return;
        }
        String raw = result.toString();
        if (raw.length() > MAX_RAW_RESULT_LENGTH) {
            raw = raw.substring(0, MAX_RAW_RESULT_LENGTH - 3) + "...";
        }
        results.add(toolMethod + ": " + raw);
    }

    @Around("@annotation(tool)")
    public Object interceptToolCall(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        String sessionId = sessionIdHolder.get();
        
        if (sessionId == null) {
            log.warn("⚠️ Tool call intercepted but no sessionId in ThreadLocal. Thread: {}", Thread.currentThread().getName());
            // No session context, proceed normally
            return joinPoint.proceed();
        }

        String toolName = getToolName(joinPoint, tool);
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        String paramsStr = formatParameters(parameters);
        log.info("🔧 [AGENT] Tool call: {} with params: {} for sessionId={}", 
                toolName, paramsStr, sessionId);
        
        long startTime = System.currentTimeMillis();
        
        // Send tool call notification
        webSocketService.sendToolCall(sessionId, toolName, parameters);
        
        // Send to unified agent activity stream
        Map<String, Object> toolCallEvent = new HashMap<>();
        toolCallEvent.put("type", "tool_call");
        toolCallEvent.put("toolName", toolName);
        toolCallEvent.put("parameters", parameters);
        toolCallEvent.put("content", "Calling " + formatToolCall(toolName, parameters));
        webSocketService.sendAgentActivity(sessionId, toolCallEvent);
        
        // Send human-friendly thinking update
        String humanFriendly = formatToolCall(toolName, parameters);
        webSocketService.sendReasoning(sessionId, "🔧 Calling tool: " + humanFriendly);
        
        try {
            // Execute the tool
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Capture the untruncated raw result for grounding verification downstream.
            recordToolResult(sessionId, joinPoint.getSignature().getName(), result);

            // Format result for logging
            String resultStr = formatResult(result);
            log.info("✅ [AGENT] Tool response: {} returned in {}ms for sessionId={}", toolName, duration, sessionId);
            log.info("📥 [AGENT] Response data: {}", resultStr);
            
            // Send tool result notification
            webSocketService.sendToolResult(sessionId, toolName, resultStr, duration);
            
            // Send to unified agent activity stream
            Map<String, Object> toolResultEvent = new HashMap<>();
            toolResultEvent.put("type", "tool_result");
            toolResultEvent.put("toolName", toolName);
            toolResultEvent.put("result", resultStr);
            toolResultEvent.put("duration", duration);
            toolResultEvent.put("content", toolName + " completed in " + duration + "ms");
            webSocketService.sendAgentActivity(sessionId, toolResultEvent);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Tool call failed: {} (duration: {}ms) for sessionId={}", toolName, duration, sessionId, e);
            
            // Send error notification
            webSocketService.sendReasoning(sessionId, 
                "❌ Tool call failed: " + toolName + " - " + e.getMessage());
            
            throw e;
        }
    }

    private String getToolName(ProceedingJoinPoint joinPoint, Tool tool) {
        // Use tool description if available, otherwise use method name
        String methodName = joinPoint.getSignature().getName();
        return formatMethodName(methodName);
    }

    private String formatMethodName(String methodName) {
        // Convert camelCase to human-readable
        return methodName
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
            .toLowerCase()
            .replaceAll("^.", String.valueOf(methodName.charAt(0)).toUpperCase());
    }

    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        String[] paramNames = getParameterNames(joinPoint);
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            Object value = args[i];
            // Don't include sensitive data or very long strings
            if (value != null) {
                String strValue = value.toString();
                if (strValue.length() > 100) {
                    params.put(paramNames[i], strValue.substring(0, 97) + "...");
                } else {
                    params.put(paramNames[i], value);
                }
            }
        }
        
        return params;
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // Try to get parameter names from method signature
        try {
            java.lang.reflect.Method method = ((org.aspectj.lang.reflect.MethodSignature) 
                joinPoint.getSignature()).getMethod();
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].getName();
            }
            return names;
        } catch (Exception e) {
            // Fallback to generic names
            Object[] args = joinPoint.getArgs();
            String[] names = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                names[i] = "param" + i;
            }
            return names;
        }
    }

    private String formatToolCall(String toolName, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(toolName);
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append(": ");
                Object value = entry.getValue();
                if (value instanceof String && ((String) value).length() > 30) {
                    sb.append("\"").append(((String) value).substring(0, 27)).append("...\"");
                } else {
                    sb.append(value);
                }
                first = false;
            }
            sb.append(")");
        }
        
        return sb.toString();
    }

    private String formatResult(Object result) {
        if (result == null) return "null";
        String str = result.toString();
        if (str.length() > 500) {
            return str.substring(0, 497) + "...";
        }
        return str;
    }
    
    private String formatParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=");
            Object value = entry.getValue();
            if (value instanceof String && ((String) value).length() > 50) {
                sb.append("\"").append(((String) value).substring(0, 47)).append("...\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        return sb.toString();
    }
}

