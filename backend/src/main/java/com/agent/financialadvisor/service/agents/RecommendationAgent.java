package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.Recommendation;
import com.agent.financialadvisor.repository.RecommendationRepository;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationAgent {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAgent.class);
    private final RecommendationRepository recommendationRepository;

    public RecommendationAgent(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    @Tool("Generate a final investment recommendation by synthesizing market analysis, risk assessment, research, and user profile. " +
          "Use this to create the final BUY/SELL/HOLD recommendation. " +
          "Requires: userId (string), symbol (string), marketAnalysis (string), riskAssessment (string), " +
          "researchSummary (string), userProfile (string). " +
          "Returns recommendation with action, confidence, and reasoning.")
    public String generateRecommendation(String userId, String symbol, String marketAnalysis, 
                                         String riskAssessment, String researchSummary, String userProfile) {
        log.info("🔵 generateRecommendation CALLED with userId={}, symbol={}", userId, symbol);
        try {
            // Parse inputs to determine recommendation
            Recommendation.RecommendationAction action = Recommendation.RecommendationAction.HOLD;
            double confidence = 0.5;
            Recommendation.RiskLevel riskLevel = Recommendation.RiskLevel.MEDIUM;
            String reasoning = "";

            // Analyze market analysis
            boolean bullishMarket = marketAnalysis.toLowerCase().contains("uptrend") || 
                                   marketAnalysis.toLowerCase().contains("bullish") ||
                                   marketAnalysis.toLowerCase().contains("positive");
            boolean bearishMarket = marketAnalysis.toLowerCase().contains("downtrend") || 
                                   marketAnalysis.toLowerCase().contains("bearish") ||
                                   marketAnalysis.toLowerCase().contains("negative");

            // Analyze risk
            boolean lowRisk = riskAssessment.toLowerCase().contains("low") && 
                            !riskAssessment.toLowerCase().contains("high");
            boolean highRisk = riskAssessment.toLowerCase().contains("high");

            if (highRisk) {
                riskLevel = Recommendation.RiskLevel.HIGH;
            } else if (lowRisk) {
                riskLevel = Recommendation.RiskLevel.LOW;
            }

            // Analyze research
            boolean strongFundamentals = researchSummary.toLowerCase().contains("strong") ||
                                        researchSummary.toLowerCase().contains("good") ||
                                        researchSummary.toLowerCase().contains("excellent") ||
                                        researchSummary.toLowerCase().contains("undervalued");
            boolean weakFundamentals = researchSummary.toLowerCase().contains("weak") ||
                                      researchSummary.toLowerCase().contains("poor") ||
                                      researchSummary.toLowerCase().contains("overvalued") ||
                                      researchSummary.toLowerCase().contains("declining");

            // Determine action and confidence
            if (bullishMarket && strongFundamentals && !highRisk) {
                action = Recommendation.RecommendationAction.BUY;
                confidence = 0.8;
                reasoning = "Strong market trends, solid fundamentals, and acceptable risk level support a BUY recommendation.";
            } else if (bearishMarket || weakFundamentals || highRisk) {
                action = Recommendation.RecommendationAction.SELL;
                confidence = 0.7;
                reasoning = "Market conditions, fundamentals, or risk factors suggest reducing exposure.";
            } else {
                action = Recommendation.RecommendationAction.HOLD;
                confidence = 0.6;
                reasoning = "Mixed signals suggest maintaining current position until clearer trends emerge.";
            }

            // Adjust confidence based on user profile compatibility
            if (userProfile.toLowerCase().contains("conservative") && highRisk) {
                confidence *= 0.7; // Lower confidence if risk doesn't match
                reasoning += " Note: Risk level may not align with conservative preferences.";
            }

            // Create recommendation entity
            Recommendation recommendation = new Recommendation();
            recommendation.setUserId(userId);
            recommendation.setSymbol(symbol);
            recommendation.setAction(action);
            recommendation.setConfidence(confidence);
            recommendation.setRiskLevel(riskLevel);
            recommendation.setReasoning(reasoning);
            recommendation.setMarketAnalysis(marketAnalysis);
            recommendation.setRiskAssessment(riskAssessment);
            recommendation.setResearchSummary(researchSummary);
            recommendation.setTimeHorizon(Recommendation.InvestmentHorizon.MEDIUM);

            recommendation = recommendationRepository.save(recommendation);

            return String.format(
                "{\"id\": %d, \"userId\": \"%s\", \"symbol\": \"%s\", \"action\": \"%s\", " +
                "\"confidence\": %.2f, \"riskLevel\": \"%s\", \"reasoning\": \"%s\", " +
                "\"message\": \"Recommendation generated and saved\"}",
                recommendation.getId(), userId, symbol, action, confidence, riskLevel, reasoning
            );
        } catch (Exception e) {
            log.error("Error generating recommendation: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error generating recommendation: %s\"}", e.getMessage());
        }
    }

    @Tool("Explain the reasoning behind a recommendation. Use this to provide detailed explanation to users. " +
          "Requires: components (JSON string with marketAnalysis, riskAssessment, researchSummary, userProfile). " +
          "Returns detailed explanation of the recommendation logic.")
    public String explainReasoning(String components) {
        log.info("🔵 explainReasoning CALLED");
        try {
            // Parse components and create explanation
            StringBuilder explanation = new StringBuilder();
            explanation.append("Recommendation Reasoning:\n\n");
            
            if (components.contains("marketAnalysis")) {
                explanation.append("Market Analysis: Based on current price trends and technical indicators, ");
                if (components.toLowerCase().contains("uptrend") || components.toLowerCase().contains("bullish")) {
                    explanation.append("the stock shows positive momentum and upward trends.\n");
                } else if (components.toLowerCase().contains("downtrend") || components.toLowerCase().contains("bearish")) {
                    explanation.append("the stock shows negative momentum and downward trends.\n");
                } else {
                    explanation.append("the stock shows mixed or neutral trends.\n");
                }
            }

            if (components.contains("riskAssessment")) {
                explanation.append("Risk Assessment: ");
                if (components.toLowerCase().contains("low risk")) {
                    explanation.append("The stock has low volatility and is considered relatively safe.\n");
                } else if (components.toLowerCase().contains("high risk")) {
                    explanation.append("The stock has high volatility and carries significant risk.\n");
                } else {
                    explanation.append("The stock has moderate risk levels.\n");
                }
            }

            if (components.contains("researchSummary")) {
                explanation.append("Fundamental Analysis: ");
                if (components.toLowerCase().contains("strong") || components.toLowerCase().contains("good")) {
                    explanation.append("Company fundamentals are strong with good financial health.\n");
                } else {
                    explanation.append("Company fundamentals require careful consideration.\n");
                }
            }

            if (components.contains("userProfile")) {
                explanation.append("User Profile Alignment: Recommendation considers your risk tolerance and investment goals.\n");
            }

            return String.format("{\"explanation\": \"%s\"}", explanation.toString().replace("\"", "\\\""));
        } catch (Exception e) {
            log.error("Error explaining reasoning: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error explaining reasoning: %s\"}", e.getMessage());
        }
    }

    @Tool("Calculate confidence score for a recommendation based on various factors. " +
          "Requires: factors (JSON string with marketSignal, riskLevel, fundamentals, userAlignment). " +
          "Returns confidence score (0.0-1.0) with explanation.")
    public String calculateConfidence(String factors) {
        log.info("🔵 calculateConfidence CALLED");
        try {
            double confidence = 0.5; // Base confidence

            // Adjust based on factors
            if (factors.toLowerCase().contains("strong") || factors.toLowerCase().contains("bullish")) {
                confidence += 0.2;
            }
            if (factors.toLowerCase().contains("low risk")) {
                confidence += 0.1;
            }
            if (factors.toLowerCase().contains("good fundamentals") || factors.toLowerCase().contains("undervalued")) {
                confidence += 0.15;
            }
            if (factors.toLowerCase().contains("aligned") || factors.toLowerCase().contains("compatible")) {
                confidence += 0.05;
            }

            // Cap at 1.0
            confidence = Math.min(1.0, confidence);

            return String.format(
                "{\"confidence\": %.2f, \"explanation\": \"Confidence calculated based on market signals, risk assessment, fundamentals, and user profile alignment.\"}",
                confidence
            );
        } catch (Exception e) {
            log.error("Error calculating confidence: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error calculating confidence: %s\"}", e.getMessage());
        }
    }

    @Tool("Format recommendation for user display. Use this to create user-friendly recommendation output. " +
          "Requires: recommendation (JSON string with action, symbol, confidence, reasoning). " +
          "Returns formatted recommendation text.")
    public String formatRecommendation(String recommendation) {
        log.info("🔵 formatRecommendation CALLED");
        try {
            // Extract key information
            String action = "HOLD";
            String confidence = "0.5";
            String reasoning = "";

            if (recommendation.contains("\"action\"")) {
                // Simple parsing (in production, use proper JSON parsing)
                if (recommendation.contains("BUY")) {
                    action = "BUY";
                } else if (recommendation.contains("SELL")) {
                    action = "SELL";
                }
            }

            StringBuilder formatted = new StringBuilder();
            formatted.append("📊 Investment Recommendation\n\n");
            formatted.append("Action: ").append(action).append("\n");
            formatted.append("Confidence: ").append(confidence).append("\n\n");
            formatted.append("Reasoning:\n").append(reasoning).append("\n");

            return String.format("{\"formatted\": \"%s\"}", formatted.toString().replace("\"", "\\\"").replace("\n", "\\n"));
        } catch (Exception e) {
            log.error("Error formatting recommendation: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error formatting recommendation: %s\"}", e.getMessage());
        }
    }
}

