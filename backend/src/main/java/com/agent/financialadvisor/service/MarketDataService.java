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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9.\\-]{1,10}$");
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String finnhubApiKey;
    private final String finnhubBaseUrl;
    private final String yahooBaseUrl;
    private final Duration timeoutDuration;
    private final long quoteCacheTtlMillis;

    /** A live quote with provenance, so callers can surface real freshness to the user. */
    public record Quote(BigDecimal price, String source, Instant quoteTime) {}

    private record CachedQuote(Quote quote, long cachedAtMillis) {}

    /** Very short-TTL cache: protects the Finnhub free tier (60/min) without serving stale prices. */
    private final Map<String, CachedQuote> quoteCache = new ConcurrentHashMap<>();

    public MarketDataService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${market-data.finnhub.api-key:}") String finnhubApiKey,
            @Value("${market-data.finnhub.base-url:https://finnhub.io/api/v1}") String finnhubBaseUrl,
            @Value("${market-data.finnhub.timeout-seconds:10}") int timeoutSeconds,
            @Value("${market-data.yahoo-finance.base-url:https://query1.finance.yahoo.com/v8/finance/chart}") String yahooBaseUrl,
            @Value("${market-data.quote-cache-ttl-seconds:15}") int quoteCacheTtlSeconds
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.finnhubApiKey = finnhubApiKey;
        this.finnhubBaseUrl = finnhubBaseUrl;
        this.yahooBaseUrl = yahooBaseUrl;
        this.timeoutDuration = Duration.ofSeconds(timeoutSeconds);
        this.quoteCacheTtlMillis = Math.max(0L, quoteCacheTtlSeconds) * 1000L;

        if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
            log.warn("⚠️ Finnhub API key is not configured. Set FINNHUB_API_KEY environment variable. " +
                    "Live quotes will fall back to Yahoo Finance.");
        } else {
            log.info("✅ MarketDataService initialized with Finnhub API (Yahoo Finance fallback enabled)");
        }
    }

    /**
     * Resolve a user-provided symbol/company name to a tradable ticker using live market APIs.
     * Returns null when no confident match can be found.
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

        String upperInput = cleanedInput.toUpperCase(Locale.ROOT);

        // If it already looks like a ticker, validate it first.
        if (isLikelySymbol(upperInput)) {
            BigDecimal directPrice = getStockPrice(upperInput);
            if (directPrice != null) {
                return upperInput;
            }
        }

        String resolved = searchBestSymbol(cleanedInput);
        if (resolved != null) {
            return resolved;
        }

        // Last resort: if user gave a ticker-like input but quote endpoint returned no data,
        // return normalized input so caller can surface a clear "unavailable/invalid" response.
        if (isLikelySymbol(upperInput)) {
            return upperInput;
        }

        log.warn("Unable to resolve tradable symbol for input: {}", symbolOrCompany);
        return null;
    }

    /**
     * Get current stock price.
     * @return Stock price, or null if symbol is invalid or all providers fail.
     */
    public BigDecimal getStockPrice(String symbol) {
        Quote quote = getQuote(symbol);
        return quote != null ? quote.price() : null;
    }

    /**
     * Get a current quote (price + source + quote time) for a symbol.
     * Tries Finnhub /quote first, then falls back to Yahoo Finance (no key required) so live prices
     * keep working even if the Finnhub key is missing or rate-limited. Cached for a few seconds to
     * protect the free-tier rate limit without serving stale data.
     */
    public Quote getQuote(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        String key = symbol.toUpperCase(Locale.ROOT);

        CachedQuote cached = quoteCache.get(key);
        if (cached != null && (System.currentTimeMillis() - cached.cachedAtMillis()) < quoteCacheTtlMillis) {
            return cached.quote();
        }

        Quote quote = fetchFinnhubQuote(key);
        if (quote == null) {
            quote = fetchYahooQuote(key);
        }
        if (quote != null) {
            quoteCache.put(key, new CachedQuote(quote, System.currentTimeMillis()));
        } else {
            log.warn("No valid price data found for symbol {} from any provider", key);
        }
        return quote;
    }

    private Quote fetchFinnhubQuote(String symbol) {
        if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
            return null;
        }
        try {
            String url = String.format("%s/quote?symbol=%s&token=%s", finnhubBaseUrl, symbol, finnhubApiKey);
            String response = webClient.get().uri(url).retrieve()
                    .bodyToMono(String.class).timeout(timeoutDuration).block();
            JsonNode json = objectMapper.readTree(response);
            if (json.has("error")) {
                log.warn("Finnhub API error for {}: {}", symbol, json.get("error").asText());
                return null;
            }
            // {"c": current, "h": high, "l": low, "o": open, "pc": prevClose, "t": epochSeconds}
            if (json.has("c") && !json.get("c").isNull()) {
                BigDecimal price = json.get("c").decimalValue();
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    long t = json.path("t").asLong(0);
                    Instant quoteTime = t > 0 ? Instant.ofEpochSecond(t) : Instant.now();
                    return new Quote(price, "finnhub", quoteTime);
                }
            }
        } catch (Exception e) {
            logFetchError("Finnhub quote", symbol, e);
        }
        return null;
    }

    private Quote fetchYahooQuote(String symbol) {
        try {
            JsonNode meta = yahooChart(symbol, null, null)
                    .path("chart").path("result").path(0).path("meta");
            JsonNode priceNode = meta.path("regularMarketPrice");
            if (!priceNode.isMissingNode() && !priceNode.isNull()) {
                BigDecimal price = priceNode.decimalValue();
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    long t = meta.path("regularMarketTime").asLong(0);
                    Instant quoteTime = t > 0 ? Instant.ofEpochSecond(t) : Instant.now();
                    log.info("Resolved live quote for {} via Yahoo Finance fallback", symbol);
                    return new Quote(price, "yahoo", quoteTime);
                }
            }
        } catch (Exception e) {
            logFetchError("Yahoo quote", symbol, e);
        }
        return null;
    }

    /**
     * Fetch the Yahoo Finance chart JSON for a symbol. range/interval may be null for a plain quote.
     * A browser-like User-Agent avoids Yahoo's bot rate limiting.
     */
    private JsonNode yahooChart(String symbol, String range, String interval) throws Exception {
        StringBuilder url = new StringBuilder(yahooBaseUrl).append("/")
                .append(URLEncoder.encode(symbol, StandardCharsets.UTF_8));
        if (range != null && interval != null) {
            url.append("?range=").append(range).append("&interval=").append(interval);
        }
        String response = webClient.get()
                .uri(url.toString())
                .header("User-Agent", "Mozilla/5.0 (compatible; FinancialAdvisor/1.0)")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeoutDuration)
                .block();
        return objectMapper.readTree(response);
    }

    private void logFetchError(String what, String symbol, Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("timeout")) {
            log.warn("Timeout fetching {} for {}: {}", what, symbol, e.getMessage());
        } else {
            log.error("Error fetching {} for {}: {}", what, symbol, e.getMessage());
        }
    }

    private String searchBestSymbol(String query) {
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
                log.warn("Finnhub API key not configured");
                return null;
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
                return null;
            }

            JsonNode results = json.path("result");
            if (!results.isArray() || results.size() == 0) {
                log.warn("No symbol search results found for query: {}", query);
                return null;
            }

            String bestSymbol = selectBestSymbolCandidate(results, query);
            if (bestSymbol != null) {
                log.info("Resolved '{}' to '{}' via live symbol search", query, bestSymbol);
            }
            return bestSymbol;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("Timeout resolving symbol for {}: {}", query, e.getMessage());
            } else {
                log.error("Error resolving symbol for {}: {}", query, e.getMessage());
            }
            return null;
        }
    }

    private String selectBestSymbolCandidate(JsonNode results, String query) {
        String normalizedQuery = query.trim().toUpperCase(Locale.ROOT);
        int bestScore = Integer.MIN_VALUE;
        String bestSymbol = null;

        for (JsonNode candidate : results) {
            String rawDisplaySymbol = candidate.path("displaySymbol").asText("");
            String rawSymbol = candidate.path("symbol").asText("");
            String chosenSymbol = !rawDisplaySymbol.isBlank() ? rawDisplaySymbol : rawSymbol;
            if (chosenSymbol == null || chosenSymbol.isBlank()) {
                continue;
            }

            String normalizedSymbol = chosenSymbol.trim().toUpperCase(Locale.ROOT);
            String normalizedRawSymbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
            String description = candidate.path("description").asText("").trim().toUpperCase(Locale.ROOT);
            String type = candidate.path("type").asText("");

            int score = 0;

            if (normalizedSymbol.equals(normalizedQuery) || normalizedRawSymbol.equals(normalizedQuery)) {
                score += 120;
            }
            if (description.equals(normalizedQuery)) {
                score += 110;
            }
            if (!description.isEmpty() && description.startsWith(normalizedQuery)) {
                score += 80;
            }
            if (!description.isEmpty() && description.contains(normalizedQuery)) {
                score += 60;
            }
            if (normalizedSymbol.startsWith(normalizedQuery)) {
                score += 50;
            }
            if ("Common Stock".equalsIgnoreCase(type) || "EQS".equalsIgnoreCase(type)) {
                score += 20;
            }
            if (!normalizedSymbol.contains(".") && !normalizedSymbol.contains(":")) {
                score += 10;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSymbol = normalizedSymbol;
            }
        }

        // Avoid selecting weak/unrelated matches from broad queries.
        if (bestScore < 50) {
            return null;
        }
        return bestSymbol;
    }

    private boolean isLikelySymbol(String value) {
        return value != null && SYMBOL_PATTERN.matcher(value).matches();
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
        
        // Basic format validation (alphanumeric, 1-10 characters)
        if (!symbol.matches("^[A-Z0-9]{1,10}$")) {
            log.warn("Symbol {} does not match valid format (1-10 alphanumeric uppercase)", symbol);
            return false;
        }
        
        // Try to fetch price to validate symbol exists
        BigDecimal price = getStockPrice(symbol);
        return price != null;
    }

    /**
     * Get stock price data for a time period. Tries Finnhub candles, then falls back to Yahoo Finance
     * (the Finnhub /stock/candle endpoint is premium-only and returns 403 on the free tier, so the
     * Yahoo fallback is what actually powers historical/technical analysis on the free plan).
     */
    public Map<String, Object> getStockPriceData(String symbol, String timeframe) {
        Map<String, Object> result = getFinnhubPriceData(symbol, timeframe);
        if (result.isEmpty()) {
            log.info("Finnhub candle data unavailable for {} ({}); trying Yahoo Finance fallback", symbol, timeframe);
            result = getYahooPriceData(symbol, timeframe);
        }
        return result;
    }

    private Map<String, Object> getFinnhubPriceData(String symbol, String timeframe) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (finnhubApiKey == null || finnhubApiKey.trim().isEmpty()) {
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
     * Historical price data via Yahoo Finance chart endpoint (no API key required).
     * Returns the same shape as getFinnhubPriceData: {symbol, data, high, low, average}.
     */
    private Map<String, Object> getYahooPriceData(String symbol, String timeframe) {
        Map<String, Object> result = new HashMap<>();
        try {
            String range = "1mo";
            String interval = "1d";
            if ("weekly".equalsIgnoreCase(timeframe)) {
                range = "1y";
                interval = "1wk";
            } else if ("monthly".equalsIgnoreCase(timeframe)) {
                range = "5y";
                interval = "1mo";
            }

            JsonNode chartResult = yahooChart(symbol.toUpperCase(Locale.ROOT), range, interval)
                    .path("chart").path("result").path(0);
            JsonNode closes = chartResult.path("indicators").path("quote").path(0).path("close");
            JsonNode highs = chartResult.path("indicators").path("quote").path(0).path("high");
            JsonNode lows = chartResult.path("indicators").path("quote").path(0).path("low");

            if (!closes.isArray() || closes.isEmpty()) {
                log.warn("No Yahoo candle data for {} ({})", symbol, timeframe);
                return result;
            }

            BigDecimal high = BigDecimal.ZERO;
            BigDecimal low = new BigDecimal("999999");
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (int i = 0; i < closes.size(); i++) {
                if (closes.get(i).isNull()) {
                    continue;
                }
                BigDecimal close = closes.get(i).decimalValue();
                BigDecimal dayHigh = (highs.isArray() && i < highs.size() && !highs.get(i).isNull())
                        ? highs.get(i).decimalValue() : close;
                BigDecimal dayLow = (lows.isArray() && i < lows.size() && !lows.get(i).isNull())
                        ? lows.get(i).decimalValue() : close;
                high = high.max(dayHigh);
                low = low.min(dayLow);
                total = total.add(close);
                count++;
            }

            if (count == 0) {
                return result;
            }

            result.put("symbol", symbol);
            result.put("source", "yahoo");
            result.put("high", high);
            result.put("low", low.compareTo(new BigDecimal("999999")) < 0 ? low : BigDecimal.ZERO);
            result.put("average", total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP));
            result.put("dataPoints", count);
        } catch (Exception e) {
            logFetchError("Yahoo price data", symbol, e);
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


