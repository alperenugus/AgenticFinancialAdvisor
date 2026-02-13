package com.agent.financialadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String alphaVantageApiKey;
    private final String alphaVantageBaseUrl;

    public MarketDataService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${market-data.alpha-vantage.api-key:demo}") String alphaVantageApiKey,
            @Value("${market-data.alpha-vantage.base-url:https://www.alphavantage.co/query}") String alphaVantageBaseUrl
    ) {
        // Build WebClient with increased buffer size for large Alpha Vantage responses
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.alphaVantageApiKey = alphaVantageApiKey;
        this.alphaVantageBaseUrl = alphaVantageBaseUrl;
    }

    /**
     * Get current stock price
     */
    public BigDecimal getStockPrice(String symbol) {
        try {
            String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                    alphaVantageBaseUrl, symbol, alphaVantageApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            JsonNode quote = json.get("Global Quote");
            if (quote != null && quote.has("05. price")) {
                return new BigDecimal(quote.get("05. price").asText());
            }
        } catch (Exception e) {
            log.error("Error fetching stock price for {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Get stock price data for a time period
     */
    public Map<String, Object> getStockPriceData(String symbol, String timeframe) {
        Map<String, Object> result = new HashMap<>();
        try {
            String function = "TIME_SERIES_DAILY";
            if ("weekly".equalsIgnoreCase(timeframe)) {
                function = "TIME_SERIES_WEEKLY";
            } else if ("monthly".equalsIgnoreCase(timeframe)) {
                function = "TIME_SERIES_MONTHLY";
            }
            
            String url = String.format("%s?function=%s&symbol=%s&apikey=%s",
                    alphaVantageBaseUrl, function, symbol, alphaVantageApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            
            // Check for API errors first
            if (json.has("Error Message") || json.has("Note")) {
                String errorMsg = json.has("Error Message") 
                    ? json.get("Error Message").asText() 
                    : json.get("Note").asText();
                log.warn("Alpha Vantage API error for {}: {}", symbol, errorMsg);
                return result; // Return empty result
            }
            
            // Find the time series key (could be "Time Series (Daily)", "Weekly Time Series", etc.)
            String timeSeriesKey = null;
            var fieldNames = json.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                if (key.contains("Time Series") || key.contains("Weekly") || key.contains("Monthly")) {
                    timeSeriesKey = key;
                    break;
                }
            }
            
            if (timeSeriesKey == null) {
                log.warn("No time series data found in response for {}", symbol);
                return result; // Return empty result
            }
            
            JsonNode timeSeries = json.get(timeSeriesKey);
            
            if (timeSeries != null && timeSeries.isObject()) {
                result.put("symbol", symbol);
                result.put("data", timeSeries);
                
                // Calculate basic statistics
                BigDecimal high = BigDecimal.ZERO;
                BigDecimal low = new BigDecimal("999999");
                BigDecimal total = BigDecimal.ZERO;
                int count = 0;
                
                for (JsonNode day : timeSeries) {
                    if (day.has("2. high")) {
                        BigDecimal dayHigh = new BigDecimal(day.get("2. high").asText());
                        BigDecimal dayLow = new BigDecimal(day.get("3. low").asText());
                        high = high.max(dayHigh);
                        low = low.min(dayLow);
                        total = total.add(dayHigh).add(dayLow);
                        count += 2;
                    }
                }
                
                result.put("high", high);
                result.put("low", low);
                result.put("average", count > 0 ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("Error fetching stock data for {}: {}", symbol, e.getMessage());
        }
        return result;
    }

    /**
     * Get company overview (fundamentals)
     */
    public Map<String, Object> getCompanyOverview(String symbol) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = String.format("%s?function=OVERVIEW&symbol=%s&apikey=%s",
                    alphaVantageBaseUrl, symbol, alphaVantageApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode json = objectMapper.readTree(response);
            if (json.has("Symbol")) {
                result.put("symbol", json.get("Symbol").asText());
                result.put("name", json.has("Name") ? json.get("Name").asText() : "");
                result.put("sector", json.has("Sector") ? json.get("Sector").asText() : "");
                result.put("peRatio", json.has("PERatio") ? json.get("PERatio").asText() : "");
                result.put("pbRatio", json.has("PriceToBookRatio") ? json.get("PriceToBookRatio").asText() : "");
                result.put("dividendYield", json.has("DividendYield") ? json.get("DividendYield").asText() : "");
                result.put("revenueGrowth", json.has("QuarterlyRevenueGrowthYOY") ? json.get("QuarterlyRevenueGrowthYOY").asText() : "");
                result.put("profitMargin", json.has("ProfitMargin") ? json.get("ProfitMargin").asText() : "");
            }
        } catch (Exception e) {
            log.error("Error fetching company overview for {}: {}", symbol, e.getMessage());
        }
        return result;
    }

    /**
     * Get market news (simplified - using Alpha Vantage news sentiment)
     */
    public String getMarketNews(String symbol) {
        try {
            String url = String.format("%s?function=NEWS_SENTIMENT&tickers=%s&apikey=%s&limit=5",
                    alphaVantageBaseUrl, symbol, alphaVantageApiKey);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse and return summary
            JsonNode json = objectMapper.readTree(response);
            if (json.has("feed") && json.get("feed").isArray()) {
                StringBuilder news = new StringBuilder();
                for (JsonNode item : json.get("feed")) {
                    if (item.has("title")) {
                        news.append(item.get("title").asText()).append(". ");
                    }
                }
                return news.toString();
            }
        } catch (Exception e) {
            log.error("Error fetching news for {}: {}", symbol, e.getMessage());
        }
        return "No recent news available.";
    }
}

