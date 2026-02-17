package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TickerResolver tickerResolver;
    private final String finnhubApiKey;
    private final String finnhubBaseUrl;
    private final Duration timeoutDuration;

    public MarketDataService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            TickerResolver tickerResolver,
            @Value("${market-data.finnhub.api-key:}") String finnhubApiKey,
            @Value("${market-data.finnhub.base-url:https://finnhub.io/api/v1}") String finnhubBaseUrl,
            @Value("${market-data.finnhub.timeout-seconds:10}") int timeoutSeconds
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.tickerResolver = tickerResolver;
        this.finnhubApiKey = finnhubApiKey;
        this.finnhubBaseUrl = finnhubBaseUrl;
        this.timeoutDuration = Duration.ofSeconds(timeoutSeconds);
        
        if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
            log.warn("⚠️ Finnhub API key is not configured. Set FINNHUB_API_KEY environment variable.");
        } else {
            log.info("✅ MarketDataService initialized with Finnhub API");
        }
    }

    /**
     * Resolve a user-provided symbol/company name to a tradable ticker using live market APIs.
     * Uses LLM-based selection over live API candidates.
     */
    public String resolveSymbol(String symbolOrCompany) {
        if (symbolOrCompany == null || symbolOrCompany.trim().isEmpty()) {
            return null;
        }

        String cleanedInput = symbolOrCompany.trim();
        if (cleanedInput.startsWith("$") && cleanedInput.length() > 1) {
            cleanedInput = cleanedInput.substring(1).trim();
        }

        if (cleanedInput.isEmpty()) {
            return null;
        }

        List<TickerResolver.Candidate> candidates = buildResolutionCandidates(cleanedInput);
        if (candidates.isEmpty()) {
            log.warn("Unable to resolve tradable symbol for input: {} (no candidates)", symbolOrCompany);
            return null;
        }

        TickerResolver.Decision decision = tickerResolver.resolve(cleanedInput, candidates);
        if (decision != null && decision.accepted() && decision.symbol() != null && !decision.symbol().isBlank()) {
            String resolved = decision.symbol().trim().toUpperCase(Locale.ROOT);
            log.info("Resolved '{}' to '{}' via LLM ticker resolver", cleanedInput, resolved);
            return resolved;
        }

        String reason = decision == null ? "no decision returned" : decision.reason();
        log.warn("Unable to resolve tradable symbol for input: {} ({})", symbolOrCompany, reason);
        return null;
    }

    /**
     * Get current stock price using Finnhub quote endpoint
     * @return Stock price, or null if symbol is invalid or error occurs
     */
    public BigDecimal getStockPrice(String symbol) {
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return null;
            }
            
            String url = String.format("%s/quote?symbol=%s&token=%s",
                    finnhubBaseUrl, symbol.toUpperCase(), finnhubApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeoutDuration)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            
            // Check for API errors
            if (json.has("error")) {
                String errorMsg = json.get("error").asText();
                log.warn("Finnhub API error for {}: {}", symbol, errorMsg);
                return null;
            }
            
            // Finnhub quote response: {"c": currentPrice, "h": high, "l": low, "o": open, "pc": previousClose, "t": timestamp}
            if (json.has("c") && !json.get("c").isNull()) {
                BigDecimal price = json.get("c").decimalValue();
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }
            }
            
            log.warn("No valid price data found for symbol: {}", symbol);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout fetching stock price for {}: {}", symbol, e.getMessage());
            } else {
                log.error("Error fetching stock price for {}: {}", symbol, e.getMessage());
            }
        }
        return null;
    }

    private List<TickerResolver.Candidate> buildResolutionCandidates(String query) {
        Map<String, TickerResolver.Candidate> candidatesBySymbol = new LinkedHashMap<>();

        String directProbeSymbol = query.toUpperCase(Locale.ROOT);
        BigDecimal directPrice = getStockPrice(directProbeSymbol);
        if (directPrice != null) {
            candidatesBySymbol.put(
                    directProbeSymbol,
                    new TickerResolver.Candidate(
                            directProbeSymbol,
                            "Direct quote probe succeeded",
                            "DIRECT_QUOTE",
                            true
                    )
            );
        }

        JsonNode results = searchSymbolCandidates(query);
        if (results != null && results.isArray()) {
            int maxCandidates = Math.min(results.size(), 20);
            for (int i = 0; i < maxCandidates; i++) {
                JsonNode candidate = results.get(i);
                String displaySymbol = candidate.path("displaySymbol").asText("").trim();
                String rawSymbol = candidate.path("symbol").asText("").trim();
                String selectedSymbol = !displaySymbol.isBlank() ? displaySymbol : rawSymbol;
                if (selectedSymbol.isBlank()) {
                    continue;
                }

                String normalizedSymbol = selectedSymbol.toUpperCase(Locale.ROOT);
                candidatesBySymbol.putIfAbsent(
                        normalizedSymbol,
                        new TickerResolver.Candidate(
                                normalizedSymbol,
                                candidate.path("description").asText(""),
                                candidate.path("type").asText(""),
                                false
                        )
                );
            }
        }

        return new ArrayList<>(candidatesBySymbol.values());
    }

    private JsonNode searchSymbolCandidates(String query) {
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return objectMapper.createArrayNode();
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s/search?q=%s&token=%s", finnhubBaseUrl, encodedQuery, finnhubApiKey);

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeoutDuration)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            if (json.has("error")) {
                log.warn("Finnhub symbol search error for {}: {}", query, json.get("error").asText());
                return objectMapper.createArrayNode();
            }

            JsonNode results = json.path("result");
            if (!results.isArray() || results.size() == 0) {
                log.warn("No symbol search results found for query: {}", query);
                return objectMapper.createArrayNode();
            }
            return results;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout resolving symbol for {}: {}", query, e.getMessage());
            } else {
                log.error("Error resolving symbol for {}: {}", query, e.getMessage());
            }
            return objectMapper.createArrayNode();
        }
    }

    /**
     * Validate if a stock symbol exists and is valid
     * @param symbol Stock symbol to validate
     * @return true if symbol is valid, false otherwise
     */
    public boolean validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }

        // Try to fetch price to validate symbol exists
        BigDecimal price = getStockPrice(symbol);
        return price != null;
    }

    /**
     * Get stock price data for a time period using Finnhub candles endpoint
     */
    public Map<String, Object> getStockPriceData(String symbol, String timeframe) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return result;
            }
            
            // Map timeframe to Finnhub resolution
            String resolution = "D"; // Daily by default
            int daysBack = 30; // Default to 30 days
            
            if ("weekly".equalsIgnoreCase(timeframe)) {
                resolution = "W";
                daysBack = 52; // 52 weeks
            } else if ("monthly".equalsIgnoreCase(timeframe)) {
                resolution = "M";
                daysBack = 12; // 12 months
            }
            
            // Calculate timestamps (Finnhub uses Unix timestamps)
            long to = Instant.now().getEpochSecond();
            long from = to - (daysBack * 24L * 60L * 60L); // daysBack days ago
            
            String url = String.format("%s/stock/candle?symbol=%s&resolution=%s&from=%d&to=%d&token=%s",
                    finnhubBaseUrl, symbol.toUpperCase(), resolution, from, to, finnhubApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeoutDuration)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            
            // Check for API errors
            if (json.has("error")) {
                String errorMsg = json.get("error").asText();
                log.warn("Finnhub API error for {}: {}", symbol, errorMsg);
                return result;
            }
            
            // Finnhub candles response: {"c": [close prices], "h": [high prices], "l": [low prices], "o": [open prices], "s": "ok", "t": [timestamps], "v": [volumes]}
            if (json.has("s") && "ok".equals(json.get("s").asText()) && json.has("c") && json.get("c").isArray()) {
                JsonNode closePrices = json.get("c");
                JsonNode highPrices = json.has("h") ? json.get("h") : null;
                JsonNode lowPrices = json.has("l") ? json.get("l") : null;
                
                result.put("symbol", symbol);
                result.put("data", json);
                
                // Calculate basic statistics
                BigDecimal high = BigDecimal.ZERO;
                BigDecimal low = new BigDecimal("999999");
                BigDecimal total = BigDecimal.ZERO;
                int count = 0;
                
                if (closePrices.isArray() && closePrices.size() > 0) {
                    for (int i = 0; i < closePrices.size(); i++) {
                        BigDecimal close = closePrices.get(i).decimalValue();
                        BigDecimal dayHigh = highPrices != null && highPrices.isArray() && i < highPrices.size() 
                            ? highPrices.get(i).decimalValue() : close;
                        BigDecimal dayLow = lowPrices != null && lowPrices.isArray() && i < lowPrices.size() 
                            ? lowPrices.get(i).decimalValue() : close;
                        
                        high = high.max(dayHigh);
                        low = low.min(dayLow);
                        total = total.add(close);
                        count++;
                    }
                }
                
                result.put("high", high.compareTo(BigDecimal.ZERO) > 0 ? high : BigDecimal.ZERO);
                result.put("low", low.compareTo(new BigDecimal("999999")) < 0 ? low : BigDecimal.ZERO);
                result.put("average", count > 0 ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
            } else {
                log.warn("No valid candle data found in response for {}", symbol);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout fetching stock data for {}: {}", symbol, e.getMessage());
            } else {
                log.error("Error fetching stock data for {}: {}", symbol, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Get company overview (fundamentals) using Finnhub company profile endpoint
     */
    public Map<String, Object> getCompanyOverview(String symbol) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return result;
            }
            
            String url = String.format("%s/stock/profile2?symbol=%s&token=%s",
                    finnhubBaseUrl, symbol.toUpperCase(), finnhubApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeoutDuration)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            
            // Check for API errors
            if (json.has("error")) {
                String errorMsg = json.get("error").asText();
                log.warn("Finnhub API error for {}: {}", symbol, errorMsg);
                return result;
            }
            
            // Finnhub profile2 response contains: name, ticker, exchange, finnhubIndustry, logo, weburl, marketCapitalization, etc.
            if (json.has("ticker")) {
                result.put("symbol", json.has("ticker") ? json.get("ticker").asText() : symbol);
                result.put("name", json.has("name") ? json.get("name").asText() : "");
                result.put("sector", json.has("finnhubIndustry") ? json.get("finnhubIndustry").asText() : "");
                result.put("exchange", json.has("exchange") ? json.get("exchange").asText() : "");
                result.put("marketCap", json.has("marketCapitalization") ? json.get("marketCapitalization").asText() : "");
                result.put("weburl", json.has("weburl") ? json.get("weburl").asText() : "");
                result.put("logo", json.has("logo") ? json.get("logo").asText() : "");
                
                // Note: Finnhub profile2 doesn't include P/E, P/B, dividend yield directly
                // These would need to be fetched from other endpoints if needed
                result.put("peRatio", "");
                result.put("pbRatio", "");
                result.put("dividendYield", "");
                result.put("revenueGrowth", "");
                result.put("profitMargin", "");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout fetching company overview for {}: {}", symbol, e.getMessage());
            } else {
                log.error("Error fetching company overview for {}: {}", symbol, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Get market news using Finnhub company news endpoint
     */
    public String getMarketNews(String symbol) {
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return "News data not available - API key not configured.";
            }
            
            // Get news from last 7 days
            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusDays(7);
            
            String url = String.format("%s/company-news?symbol=%s&from=%s&to=%s&token=%s",
                    finnhubBaseUrl, symbol.toUpperCase(), fromDate.toString(), toDate.toString(), finnhubApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeoutDuration)
                    .block();
            
            // Parse and return summary
            JsonNode json = objectMapper.readTree(response);
            
            // Check for API errors
            if (json.has("error")) {
                String errorMsg = json.get("error").asText();
                log.warn("Finnhub API error for news {}: {}", symbol, errorMsg);
                return "No recent news available.";
            }
            
            // Finnhub company-news returns an array of news items
            if (json.isArray() && json.size() > 0) {
                StringBuilder news = new StringBuilder();
                int count = 0;
                int maxItems = Math.min(5, json.size()); // Limit to 5 items
                
                for (int i = 0; i < maxItems; i++) {
                    JsonNode item = json.get(i);
                    if (item.has("headline")) {
                        news.append(item.get("headline").asText());
                        if (item.has("summary") && !item.get("summary").isNull()) {
                            String summary = item.get("summary").asText();
                            if (summary.length() > 100) {
                                summary = summary.substring(0, 97) + "...";
                            }
                            news.append(" - ").append(summary);
                        }
                        news.append(". ");
                        count++;
                    }
                }
                
                if (count > 0) {
                    return news.toString();
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout fetching news for {}: {}", symbol, e.getMessage());
                return "News data request timed out. Please try again.";
            } else {
                log.error("Error fetching news for {}: {}", symbol, e.getMessage());
            }
        }
        return "No recent news available.";
    }
}


