package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.StockHolding;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.service.MarketDataService;
import com.agent.financialadvisor.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/portfolio")
// CORS handled by WebConfig - no need for @CrossOrigin here
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);
    private final PortfolioRepository portfolioRepository;
    private final MarketDataService marketDataService;

    public PortfolioController(
            PortfolioRepository portfolioRepository,
            MarketDataService marketDataService
    ) {
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
    }

    /**
     * Get user portfolio for authenticated user
     * GET /api/portfolio
     */
    @GetMapping
    public ResponseEntity<Portfolio> getPortfolio() {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                // Create empty portfolio if doesn't exist
                Portfolio portfolio = new Portfolio();
                portfolio.setUserId(userId);
                portfolio = portfolioRepository.save(portfolio);
                return ResponseEntity.ok(portfolio);
            }

            Portfolio portfolio = portfolioOpt.get();
            // Update current prices
            updatePortfolioPrices(portfolio);
            portfolio = portfolioRepository.save(portfolio);
            
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            log.error("Error getting portfolio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add holding to portfolio for authenticated user
     * POST /api/portfolio/holdings
     */
    @PostMapping("/holdings")
    public ResponseEntity<Map<String, Object>> addHolding(@RequestBody Map<String, Object> request) {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            // Validate request parameters
            if (request.get("symbol") == null || request.get("quantity") == null || request.get("averagePrice") == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Missing required fields: symbol, quantity, averagePrice"));
            }

            Portfolio portfolio = portfolioRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        Portfolio p = new Portfolio();
                        p.setUserId(userId);
                        return portfolioRepository.save(p);
                    });

            String symbol = ((String) request.get("symbol")).toUpperCase().trim();
            if (symbol.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Symbol cannot be empty"));
            }

            Integer quantity;
            try {
                quantity = ((Number) request.get("quantity")).intValue();
                if (quantity <= 0) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Quantity must be greater than 0"));
                }
            } catch (ClassCastException | NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid quantity format"));
            }

            BigDecimal averagePrice;
            try {
                averagePrice = new BigDecimal(request.get("averagePrice").toString());
                if (averagePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Average price must be greater than 0"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid average price format"));
            }

            // Check if holding already exists
            Optional<StockHolding> existingHolding = portfolio.getHoldings().stream()
                    .filter(h -> h.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst();

            StockHolding holding;
            if (existingHolding.isPresent()) {
                // Update existing holding (average the prices)
                holding = existingHolding.get();
                int oldQuantity = holding.getQuantity();
                BigDecimal oldAvgPrice = holding.getAveragePrice();
                
                // Calculate new average price
                BigDecimal totalCost = oldAvgPrice.multiply(BigDecimal.valueOf(oldQuantity))
                        .add(averagePrice.multiply(BigDecimal.valueOf(quantity)));
                BigDecimal newQuantity = BigDecimal.valueOf(oldQuantity + quantity);
                BigDecimal newAvgPrice = totalCost.divide(newQuantity, 2, java.math.RoundingMode.HALF_UP);
                
                holding.setQuantity(oldQuantity + quantity);
                holding.setAveragePrice(newAvgPrice);
                log.info("Updating existing holding: {} - New quantity: {}, New avg price: {}", 
                    symbol, holding.getQuantity(), newAvgPrice);
            } else {
                // Create new holding
                holding = new StockHolding();
                holding.setPortfolioId(portfolio.getId());
                holding.setSymbol(symbol);
                holding.setQuantity(quantity);
                holding.setAveragePrice(averagePrice);
                portfolio.getHoldings().add(holding);
                log.info("Creating new holding: {} - Quantity: {}, Avg price: {}", 
                    symbol, quantity, averagePrice);
            }

            // Fetch and update current price
            try {
                BigDecimal currentPrice = marketDataService.getStockPrice(symbol);
                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    holding.setCurrentPrice(currentPrice);
                    log.info("Set current price for {}: {}", symbol, currentPrice);
                } else {
                    log.warn("Could not fetch current price for {}, setting to average price", symbol);
                    holding.setCurrentPrice(averagePrice);
                }
            } catch (Exception e) {
                log.warn("Error fetching current price for {}: {}", symbol, e.getMessage());
                // Set current price to average price as fallback
                holding.setCurrentPrice(averagePrice);
            }

            // Save portfolio (cascade will save the holding)
            portfolio = portfolioRepository.save(portfolio);
            
            // Refresh the holding to get calculated values
            holding = portfolio.getHoldings().stream()
                    .filter(h -> h.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(holding);

            log.info("Holding saved successfully: {} - Value: {}, Gain/Loss: {}", 
                symbol, holding.getValue(), holding.getGainLoss());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Holding added successfully");
            response.put("holding", holding);
            response.put("portfolio", portfolio);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding holding: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error adding holding: " + e.getMessage()));
        }
    }

    /**
     * Remove holding from portfolio for authenticated user
     * DELETE /api/portfolio/holdings/{holdingId}
     */
    @DeleteMapping("/holdings/{holdingId}")
    public ResponseEntity<Map<String, Object>> removeHolding(@PathVariable Long holdingId) {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Portfolio portfolio = portfolioOpt.get();
            boolean removed = portfolio.getHoldings().removeIf(h -> h.getId().equals(holdingId));
            
            if (!removed) {
                return ResponseEntity.notFound().build();
            }

            portfolio = portfolioRepository.save(portfolio);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Holding removed successfully");
            response.put("portfolio", portfolio);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error removing holding: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error removing holding: " + e.getMessage()));
        }
    }

    /**
     * Update portfolio prices (refresh current prices for all holdings) for authenticated user
     * POST /api/portfolio/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Portfolio> refreshPortfolio() {
        try {
            String userId = SecurityUtil.getCurrentUserEmail()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));
            
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserId(userId);
            if (portfolioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Portfolio portfolio = portfolioOpt.get();
            updatePortfolioPrices(portfolio);
            portfolio = portfolioRepository.save(portfolio);

            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            log.error("Error refreshing portfolio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update current prices for all holdings in portfolio
     */
    private void updatePortfolioPrices(Portfolio portfolio) {
        if (portfolio.getHoldings() != null) {
            for (StockHolding holding : portfolio.getHoldings()) {
                try {
                    BigDecimal currentPrice = marketDataService.getStockPrice(holding.getSymbol());
                    if (currentPrice != null) {
                        holding.setCurrentPrice(currentPrice);
                    }
                } catch (Exception e) {
                    log.warn("Could not update price for {}: {}", holding.getSymbol(), e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}

