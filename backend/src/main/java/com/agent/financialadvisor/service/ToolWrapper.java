package com.agent.financialadvisor.service;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps tool objects to intercept method calls and send WebSocket updates
 * This is needed because LangChain4j calls tools via reflection, bypassing Spring AOP
 */
@Component
public class ToolWrapper {

    private static final Logger log = LoggerFactory.getLogger(ToolWrapper.class);
    private final WebSocketService webSocketService;
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();

    @Autowired
    public ToolWrapper(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    public static void setSessionId(String sessionId) {
        sessionIdHolder.set(sessionId);
    }

    public static void clearSessionId() {
        sessionIdHolder.remove();
    }

    /**
     * Wrap a tool object with a proxy that intercepts method calls
     */
    @SuppressWarnings("unchecked")
    public <T> T wrapTool(T tool, Class<T> toolInterface) {
        String sessionId = sessionIdHolder.get();
        if (sessionId == null) {
            // No session context, return original
            return tool;
        }

        return (T) Proxy.newProxyInstance(
            toolInterface.getClassLoader(),
            new Class[]{toolInterface},
            new ToolInvocationHandler(tool, sessionId)
        );
    }

    private class ToolInvocationHandler implements InvocationHandler {
        private final Object target;
        private final String sessionId;

        public ToolInvocationHandler(Object target, String sessionId) {
            this.target = target;
            this.sessionId = sessionId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Check if method has @Tool annotation
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation == null) {
                // Not a tool method, proceed normally
                return method.invoke(target, args);
            }

            String toolName = formatMethodName(method.getName());
            Map<String, Object> parameters = extractParameters(method, args);
            
            long startTime = System.currentTimeMillis();
            
            // Send tool call notification
            log.info("🔧 Tool called: {} with params: {}", toolName, parameters);
            webSocketService.sendToolCall(sessionId, toolName, parameters);
            
            // Send human-friendly thinking update
            String humanFriendly = formatToolCall(toolName, parameters);
            webSocketService.sendReasoning(sessionId, "🔧 Using: " + humanFriendly);
            
            try {
                // Execute the tool
                Object result = method.invoke(target, args);
                
                long duration = System.currentTimeMillis() - startTime;
                
                // Send tool result notification
                webSocketService.sendToolResult(sessionId, toolName, 
                    formatResult(result), duration);
                
                log.info("✅ Tool completed: {} in {}ms", toolName, duration);
                
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("❌ Tool call failed: {} (duration: {}ms)", toolName, duration, e);
                
                // Send error notification
                webSocketService.sendReasoning(sessionId, 
                    "❌ Tool failed: " + toolName + " - " + e.getMessage());
                
                throw e;
            }
        }

        private String formatMethodName(String methodName) {
            return methodName
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .toLowerCase()
                .replaceAll("^.", String.valueOf(methodName.charAt(0)).toUpperCase());
        }

        private Map<String, Object> extractParameters(Method method, Object[] args) {
            Map<String, Object> params = new HashMap<>();
            if (args == null || args.length == 0) {
                return params;
            }

            java.lang.reflect.Parameter[] parameters = method.getParameters();
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                Object value = args[i];
                if (value != null) {
                    String strValue = value.toString();
                    if (strValue.length() > 100) {
                        params.put(parameters[i].getName(), strValue.substring(0, 97) + "...");
                    } else {
                        params.put(parameters[i].getName(), value);
                    }
                }
            }
            return params;
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
}

