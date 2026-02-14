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

import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect to intercept tool calls and send updates via WebSocket
 * This allows users to see what tools the agent is using in real-time
 */
@Aspect
@Component
@Order(1)
public class ToolCallAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAspect.class);
    private final WebSocketService webSocketService;
    
    // Thread-local storage for session ID (set by OrchestratorService)
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();

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

    @Around("@annotation(tool)")
    public Object interceptToolCall(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        String sessionId = sessionIdHolder.get();
        if (sessionId == null) {
            // No session context, proceed normally
            return joinPoint.proceed();
        }

        String toolName = getToolName(joinPoint, tool);
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        long startTime = System.currentTimeMillis();
        
        // Send tool call notification
        webSocketService.sendToolCall(sessionId, toolName, parameters);
        
        // Send human-friendly thinking update
        String humanFriendly = formatToolCall(toolName, parameters);
        webSocketService.sendReasoning(sessionId, "Using tool: " + humanFriendly);
        
        try {
            // Execute the tool
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Send tool result notification
            webSocketService.sendToolResult(sessionId, toolName, 
                formatResult(result), duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Tool call failed: {} (duration: {}ms)", toolName, duration, e);
            
            // Send error notification
            webSocketService.sendReasoning(sessionId, 
                "Tool call failed: " + toolName + " - " + e.getMessage());
            
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
        if (str.length() > 200) {
            return str.substring(0, 197) + "...";
        }
        return str;
    }
}

