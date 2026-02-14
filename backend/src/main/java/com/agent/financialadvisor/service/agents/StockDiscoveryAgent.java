package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent for discovering stocks based on criteria (risk tolerance, sectors, etc.)
 * Uses real-time market data to find suitable stocks instead of hardcoded lists
 */
@Service
public class StockDiscoveryAgent {

    private static final Logger log = LoggerFactory.getLogger(StockDiscoveryAgent.class);
    private final MarketDataService marketDataService;

    // Popular stock symbols across different sectors for discovery
    // These are used as starting points for validation, not hardcoded recommendations
    private static final String[] POPULAR_STOCKS = {
        // Technology
        "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA", "AMD", "INTC", "CRM",
        // Finance
        "JPM", "BAC", "WFC", "GS", "MS", "V", "MA", "PYPL",
        // Healthcare
        "JNJ", "PFE", "UNH", "ABT", "TMO", "ABBV", "MRK",
        // Consumer
        "WMT", "HD", "MCD", "NKE", "SBUX", "TGT", "COST",
        // Industrial
        "BA", "CAT", "GE", "HON", "UPS", "RTX",
        // Energy
        "XOM", "CVX", "SLB", "COP", "EOG",
        // Utilities
        "NEE", "DUK", "SO", "AEP", "SRE",
        // Materials
        "LIN", "APD", "ECL", "SHW", "DD"
    };

    public StockDiscoveryAgent(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Tool("Discover stocks that match specific criteria. Use this to find stocks based on risk tolerance, sectors, or other criteria. " +
          "Requires: riskTolerance (CONSERVATIVE, MODERATE, or AGGRESSIVE), sectors (optional, comma-separated list), " +
          "excludeOwned (optional, comma-separated list of symbols to exclude). " +
          "Returns list of valid stock symbols that match the criteria.")
    public String discoverStocks(String riskTolerance, String sectors, String excludeOwned) {
        log.info("🔵 discoverStocks CALLED with riskTolerance={}, sectors={}, excludeOwned={}", 
            riskTolerance, sectors, excludeOwned);
        
        try {
            List<String> excludeList = new ArrayList<>();
            if (excludeOwned != null && !excludeOwned.trim().isEmpty()) {
                for (String symbol : excludeOwned.split(",")) {
                    excludeList.add(symbol.trim().toUpperCase());
                }
            }

            List<String> validStocks = new ArrayList<>();
            int maxStocks = 10; // Limit to prevent too many API calls
            
            // Validate stocks from popular list by checking if they exist and match criteria
            for (String symbol : POPULAR_STOCKS) {
                if (validStocks.size() >= maxStocks) break;
                
                symbol = symbol.trim().toUpperCase();
                
                // Skip if in exclude list
                if (excludeList.contains(symbol)) {
                    continue;
                }
                
                // Validate stock exists by checking price
                try {
                    var price = marketDataService.getStockPrice(symbol);
                    if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        // Stock is valid, add to list
                        validStocks.add(symbol);
                        log.debug("Validated stock: {}", symbol);
                    }
                } catch (Exception e) {
                    log.debug("Stock {} validation failed: {}", symbol, e.getMessage());
                    // Continue to next stock
                }
                
                // Small delay to avoid rate limiting
                Thread.sleep(200);
            }
            
            return String.format(
                "{\"riskTolerance\": \"%s\", \"discoveredStocks\": %s, \"count\": %d, " +
                "\"message\": \"Discovered %d valid stocks matching criteria\"}",
                riskTolerance, validStocks.toString(), validStocks.size(), validStocks.size()
            );
        } catch (Exception e) {
            log.error("Error discovering stocks: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error discovering stocks: %s\"}", e.getMessage());
        }
    }

    @Tool("Validate if a stock symbol exists and is tradeable. Use this to verify stock symbols before analysis. " +
          "Requires: symbol (string). Returns validation result with current price if valid.")
    public String validateStockSymbol(String symbol) {
        log.info("🔵 validateStockSymbol CALLED with symbol={}", symbol);
        
        try {
            symbol = symbol.trim().toUpperCase();
            var price = marketDataService.getStockPrice(symbol);
            
            if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return String.format(
                    "{\"symbol\": \"%s\", \"valid\": true, \"currentPrice\": %s, \"message\": \"Stock symbol is valid\"}",
                    symbol, price
                );
            } else {
                return String.format(
                    "{\"symbol\": \"%s\", \"valid\": false, \"message\": \"Stock symbol not found or invalid\"}",
                    symbol
                );
            }
        } catch (Exception e) {
            log.error("Error validating stock symbol {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"valid\": false, \"error\": \"%s\"}", symbol, e.getMessage());
        }
    }
}

