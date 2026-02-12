package com.agent.financialadvisor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

