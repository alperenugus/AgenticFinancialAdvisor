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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataServiceSymbolSelectionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer httpServer;
    private MarketDataService service;
    private StubTickerResolver tickerResolver;
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
        tickerResolver = new StubTickerResolver();
        service = new MarketDataService(
                WebClient.builder(),
                objectMapper,
                tickerResolver,
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

        tickerResolver.nextDecision = new TickerResolver.Decision("FIG", true, "Best company identity match");
        String resolved = service.resolveSymbol("Figma");

        assertThat(resolved).isEqualTo("FIG");
        assertThat(tickerResolver.lastInput).isEqualTo("Figma");
        assertThat(tickerResolver.lastCandidates).extracting(TickerResolver.Candidate::symbol)
                .contains("FIG", "FIGS");
    }

    @Test
    void resolveSymbol_ReturnsNullWhenResolverRejectsCandidates() {
        quoteResponses.put("FIGMA", "{\"c\":0}");
        searchResponse = """
                {
                  "result": [
                    {"description":"FIGS INC","displaySymbol":"FIGS","symbol":"FIGS","type":"Common Stock"},
                    {"description":"FIGEAC AEROSPACE","displaySymbol":"FIGE","symbol":"FIGE","type":"Common Stock"}
                  ]
                }
                """;

        tickerResolver.nextDecision = new TickerResolver.Decision(null, false, "No confident match");
        String resolved = service.resolveSymbol("Figma");

        assertThat(resolved).isNull();
    }

    @Test
    void resolveSymbol_UsesDirectQuoteForTickerInput() {
        quoteResponses.put("TSLA", "{\"c\":300.5}");

        tickerResolver.nextDecision = new TickerResolver.Decision("TSLA", true, "Direct quote candidate");
        String resolved = service.resolveSymbol("TSLA");

        assertThat(resolved).isEqualTo("TSLA");
        assertThat(tickerResolver.lastCandidates).extracting(TickerResolver.Candidate::symbol).contains("TSLA");
        assertThat(tickerResolver.lastCandidates).anyMatch(TickerResolver.Candidate::directQuoteCandidate);
    }

    @Test
    void resolveSymbol_ReturnsNullWhenNoCandidatesFromQuoteOrSearch() {
        quoteResponses.put("ABCD", "{\"c\":0}");
        tickerResolver.nextDecision = new TickerResolver.Decision("ABCD", true, "Should not be used");

        String resolved = service.resolveSymbol("ABCD");

        assertThat(resolved).isNull();
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

    private static class StubTickerResolver implements TickerResolver {
        private Decision nextDecision = new Decision(null, false, "unset");
        private String lastInput = "";
        private List<Candidate> lastCandidates = List.of();

        @Override
        public Decision resolve(String userInput, List<Candidate> candidates) {
            this.lastInput = userInput;
            this.lastCandidates = candidates;
            return nextDecision;
        }
    }
}
