package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    public ResearchAgent(MarketDataService marketDataService, ObjectMapper objectMapper) {
        this.marketDataService = marketDataService;
        this.objectMapper = objectMapper;
    }

    @Tool("Get company fundamentals including P/E ratio, P/B ratio, dividend yield, revenue growth, and profit margin. " +
          "Use this for fundamental analysis of a company. " +
          "Requires: symbol (string). Returns comprehensive fundamental data.")
    public String getCompanyFundamentals(String symbol) {
        log.info("🔵 getCompanyFundamentals CALLED with symbol={}", symbol);
        try {
            Map<String, Object> overview = marketDataService.getCompanyOverview(symbol);
            if (overview.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to fetch company fundamentals.\"}", symbol);
            }

            // Convert to JSON string for LLM
            String json = objectMapper.writeValueAsString(overview);
            return json;
        } catch (Exception e) {
            log.error("Error getting company fundamentals for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error fetching fundamentals: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Analyze financial statements and ratios. Use this to evaluate company financial health. " +
          "Requires: symbol (string). Returns financial analysis with key metrics and interpretations.")
    public String analyzeFinancials(String symbol) {
        log.info("🔵 analyzeFinancials CALLED with symbol={}", symbol);
        try {
            Map<String, Object> fundamentals = marketDataService.getCompanyOverview(symbol);
            if (fundamentals.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to analyze financials - no data available.\"}", symbol);
            }

            StringBuilder analysis = new StringBuilder();
            analysis.append("Financial Analysis for ").append(symbol).append(":\n");

            // P/E Ratio analysis
            String peRatio = (String) fundamentals.get("peRatio");
            if (peRatio != null && !peRatio.isEmpty() && !peRatio.equals("None")) {
                try {
                    double pe = Double.parseDouble(peRatio);
                    if (pe < 15) {
                        analysis.append("P/E Ratio: ").append(pe).append(" (Undervalued - good buying opportunity)\n");
                    } else if (pe < 25) {
                        analysis.append("P/E Ratio: ").append(pe).append(" (Fairly valued)\n");
                    } else {
                        analysis.append("P/E Ratio: ").append(pe).append(" (Potentially overvalued)\n");
                    }
                } catch (NumberFormatException e) {
                    analysis.append("P/E Ratio: ").append(peRatio).append("\n");
                }
            }

            // Profit Margin analysis
            String profitMargin = (String) fundamentals.get("profitMargin");
            if (profitMargin != null && !profitMargin.isEmpty() && !profitMargin.equals("None")) {
                try {
                    double margin = Double.parseDouble(profitMargin);
                    if (margin > 20) {
                        analysis.append("Profit Margin: ").append(margin).append("% (Excellent profitability)\n");
                    } else if (margin > 10) {
                        analysis.append("Profit Margin: ").append(margin).append("% (Good profitability)\n");
                    } else {
                        analysis.append("Profit Margin: ").append(margin).append("% (Low profitability)\n");
                    }
                } catch (NumberFormatException e) {
                    analysis.append("Profit Margin: ").append(profitMargin).append("\n");
                }
            }

            // Revenue Growth analysis
            String revenueGrowth = (String) fundamentals.get("revenueGrowth");
            if (revenueGrowth != null && !revenueGrowth.isEmpty() && !revenueGrowth.equals("None")) {
                try {
                    double growth = Double.parseDouble(revenueGrowth);
                    if (growth > 20) {
                        analysis.append("Revenue Growth: ").append(growth).append("% (Strong growth)\n");
                    } else if (growth > 0) {
                        analysis.append("Revenue Growth: ").append(growth).append("% (Moderate growth)\n");
                    } else {
                        analysis.append("Revenue Growth: ").append(growth).append("% (Declining revenue)\n");
                    }
                } catch (NumberFormatException e) {
                    analysis.append("Revenue Growth: ").append(revenueGrowth).append("\n");
                }
            }

            // Dividend Yield
            String dividendYield = (String) fundamentals.get("dividendYield");
            if (dividendYield != null && !dividendYield.isEmpty() && !dividendYield.equals("None")) {
                analysis.append("Dividend Yield: ").append(dividendYield).append("\n");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("analysis", analysis.toString());
            result.put("fundamentals", fundamentals);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error analyzing financials for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error analyzing financials: %s\"}", symbol, e.getMessage());
        }
    }

    @Tool("Compare multiple companies side by side. Use this to evaluate investment options. " +
          "Requires: symbols (comma-separated string, e.g., 'AAPL,MSFT,GOOGL'). " +
          "Returns comparison of key metrics.")
    public String compareCompanies(String symbols) {
        log.info("🔵 compareCompanies CALLED with symbols={}", symbols);
        try {
            String[] symbolArray = symbols.split(",");
            List<Map<String, Object>> comparisons = new ArrayList<>();

            for (String symbol : symbolArray) {
                symbol = symbol.trim().toUpperCase();
                Map<String, Object> overview = marketDataService.getCompanyOverview(symbol);
                if (!overview.isEmpty()) {
                    comparisons.add(overview);
                }
            }

            if (comparisons.isEmpty()) {
                return String.format("{\"symbols\": \"%s\", \"error\": \"Unable to compare companies - no data available.\"}", symbols);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("symbols", symbols);
            result.put("comparisons", comparisons);
            result.put("count", comparisons.size());

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error comparing companies {}: {}", symbols, e.getMessage(), e);
            return String.format("{\"symbols\": \"%s\", \"error\": \"Error comparing companies: %s\"}", symbols, e.getMessage());
        }
    }

    @Tool("Get sector analysis and industry comparison. Use this to understand how a company performs relative to its sector. " +
          "Requires: symbol (string). Returns sector information and industry context.")
    public String getSectorAnalysis(String symbol) {
        log.info("🔵 getSectorAnalysis CALLED with symbol={}", symbol);
        try {
            Map<String, Object> overview = marketDataService.getCompanyOverview(symbol);
            if (overview.isEmpty()) {
                return String.format("{\"symbol\": \"%s\", \"error\": \"Unable to get sector analysis.\"}", symbol);
            }

            String sector = (String) overview.get("sector");
            String name = (String) overview.get("name");

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("companyName", name != null ? name : "");
            result.put("sector", sector != null ? sector : "Unknown");
            result.put("fundamentals", overview);
            result.put("message", sector != null ? 
                String.format("Company operates in %s sector. Use compareCompanies to compare with sector peers.", sector) :
                "Sector information not available.");

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error getting sector analysis for {}: {}", symbol, e.getMessage(), e);
            return String.format("{\"symbol\": \"%s\", \"error\": \"Error getting sector analysis: %s\"}", symbol, e.getMessage());
        }
    }
}

