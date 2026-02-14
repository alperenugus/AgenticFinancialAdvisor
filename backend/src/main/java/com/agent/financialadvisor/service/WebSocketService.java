package com.agent.financialadvisor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendThinking(String sessionId, String thinking) {
        try {
            messagingTemplate.convertAndSend("/topic/thinking/" + sessionId,
                    new ThinkingMessage("thinking", thinking));
            log.debug("Sent thinking to session {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending thinking via WebSocket", e);
        }
    }

    public void sendFinalResponse(String sessionId, String response) {
        try {
            messagingTemplate.convertAndSend("/topic/response/" + sessionId,
                    new ThinkingMessage("response", response));
            log.debug("Sent final response to session {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending final response via WebSocket", e);
        }
    }

    public void sendError(String sessionId, String error) {
        try {
            messagingTemplate.convertAndSend("/topic/error/" + sessionId,
                    new ThinkingMessage("error", error));
            log.debug("Sent error to session {}: {}", sessionId, error);
        } catch (Exception e) {
            log.error("Error sending error via WebSocket", e);
        }
    }

    public void sendToolCall(String sessionId, String toolName, Map<String, Object> parameters) {
        try {
            Map<String, Object> toolCall = new java.util.HashMap<>();
            toolCall.put("type", "tool_call");
            toolCall.put("toolName", toolName);
            toolCall.put("parameters", parameters);
            toolCall.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/tool-call/" + sessionId, toolCall);
            log.debug("Sent tool call to session {}: {}", sessionId, toolName);
        } catch (Exception e) {
            log.error("Error sending tool call via WebSocket", e);
        }
    }

    public void sendToolResult(String sessionId, String toolName, Object result, long duration) {
        try {
            Map<String, Object> toolResult = new java.util.HashMap<>();
            toolResult.put("type", "tool_result");
            toolResult.put("toolName", toolName);
            toolResult.put("result", result);
            toolResult.put("duration", duration);
            toolResult.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/tool-result/" + sessionId, toolResult);
            log.debug("Sent tool result to session {}: {}", sessionId, toolName);
        } catch (Exception e) {
            log.error("Error sending tool result via WebSocket", e);
        }
    }

    public void sendReasoning(String sessionId, String reasoning) {
        try {
            Map<String, Object> reasoningMsg = new java.util.HashMap<>();
            reasoningMsg.put("type", "reasoning");
            reasoningMsg.put("content", reasoning);
            reasoningMsg.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/reasoning/" + sessionId, reasoningMsg);
            log.debug("Sent reasoning to session {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending reasoning via WebSocket", e);
        }
    }

    public static class ThinkingMessage {
        private String type;
        private String content;

        public ThinkingMessage() {}

        public ThinkingMessage(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}


