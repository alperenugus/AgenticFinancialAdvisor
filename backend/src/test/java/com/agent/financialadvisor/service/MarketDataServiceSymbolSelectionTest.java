package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataServiceSymbolSelectionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer httpServer;
    private MarketDataService service;
    private final Map<String, String> quoteResponses = new ConcurrentHashMap<>();
    private volatile String searchResponse;

    @BeforeEach
    void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/quote", exchange -> {
            String symbol = queryParam(exchange.getRequestURI().getQuery(), "symbol");
            String body = quoteResponses.getOrDefault(
                    symbol == null ? "" : symbol.toUpperCase(),
                    "{\"c\":0}"
            );
            writeJson(exchange, body);
        });
        httpServer.createContext("/search", exchange -> writeJson(exchange, searchResponse));
        httpServer.start();

        searchResponse = "{\"result\":[]}";
        service = new MarketDataService(
                WebClient.builder(),
                objectMapper,
                "test-key",
                "http://localhost:" + httpServer.getAddress().getPort(),
                10
        );
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void resolveSymbol_PrefersCompanyIdentityOverLookalikeTicker() {
        quoteResponses.put("FIGMA", "{\"c\":0}");
        searchResponse = """
                {
                  "result": [
                    {"description":"FIGS INC","displaySymbol":"FIGS","symbol":"FIGS","type":"Common Stock"},
                    {"description":"Figma Inc","displaySymbol":"FIG","symbol":"FIG","type":"Common Stock"}
                  ]
                }
                """;

        String resolved = service.resolveSymbol("Figma");

        assertThat(resolved).isEqualTo("FIG");
    }

    @Test
    void resolveSymbol_ReturnsNullWhenOnlyLowConfidenceCandidatesExist() {
        quoteResponses.put("FIGMA", "{\"c\":0}");
        searchResponse = """
                {
                  "result": [
                    {"description":"FIGS INC","displaySymbol":"FIGS","symbol":"FIGS","type":"Common Stock"},
                    {"description":"FIGEAC AEROSPACE","displaySymbol":"FIGE","symbol":"FIGE","type":"Common Stock"}
                  ]
                }
                """;

        String resolved = service.resolveSymbol("Figma");

        assertThat(resolved).isNull();
    }

    @Test
    void resolveSymbol_UsesDirectQuoteForTickerInput() {
        quoteResponses.put("TSLA", "{\"c\":300.5}");

        String resolved = service.resolveSymbol("TSLA");

        assertThat(resolved).isEqualTo("TSLA");
    }

    @Test
    void resolveSymbol_TickerLikeInputFallsBackToNormalizedTickerWhenUnavailable() {
        quoteResponses.put("ABCD", "{\"c\":0}");

        String resolved = service.resolveSymbol("ABCD");

        assertThat(resolved).isEqualTo("ABCD");
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = (body == null ? "{}" : body).getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String queryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }
}
