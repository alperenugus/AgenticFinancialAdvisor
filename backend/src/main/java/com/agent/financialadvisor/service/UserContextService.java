package com.agent.financialadvisor.service;

import com.agent.financialadvisor.model.Portfolio;
import com.agent.financialadvisor.model.StockHolding;
import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.PortfolioRepository;
import com.agent.financialadvisor.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds a compact, authoritative USER PROFILE CONTEXT block that the orchestrator injects into
 * BOTH the planner input and the evaluator input on every query.
 *
 * Rationale: previously, user preferences only reached the answer if the LLM planner chose to
 * schedule a USER_PROFILE step — a discretionary hop that silently broke personalization whenever
 * the planner skipped it. Loading the profile deterministically from the DB (one cheap read)
 * guarantees risk tolerance, goals, horizon, budget, sector preferences, and ESG settings are
 * always available to the agents that plan and write the final answer.
 */
@Service
public class UserContextService {

    private static final Logger log = LoggerFactory.getLogger(UserContextService.class);

    private final UserProfileRepository userProfileRepository;
    private final PortfolioRepository portfolioRepository;

    public UserContextService(UserProfileRepository userProfileRepository,
                              PortfolioRepository portfolioRepository) {
        this.userProfileRepository = userProfileRepository;
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * Compact context block for prompt injection. Never throws — personalization must not be able
     * to take down the main answer path.
     */
    @Transactional(readOnly = true)
    public String buildProfileContext(String userId) {
        try {
            StringBuilder sb = new StringBuilder("USER PROFILE CONTEXT (authoritative, from database):\n");

            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                sb.append("- No investment profile on file. For advice queries, note that recommendations are ")
                  .append("generic and suggest completing the profile for personalized guidance.\n");
            } else {
                UserProfile p = profileOpt.get();
                List<String> goals = p.getGoals() != null ? new ArrayList<>(p.getGoals()) : new ArrayList<>();
                List<String> preferred = p.getPreferredSectors() != null ? new ArrayList<>(p.getPreferredSectors()) : new ArrayList<>();
                List<String> excluded = p.getExcludedSectors() != null ? new ArrayList<>(p.getExcludedSectors()) : new ArrayList<>();
                sb.append("- Risk tolerance: ").append(p.getRiskTolerance())
                  .append("; Investment horizon: ").append(p.getHorizon())
                  .append("; Goals: ").append(goals.isEmpty() ? "not set" : String.join(", ", goals)).append('\n');
                sb.append("- Budget: ").append(p.getBudget() != null ? "$" + p.getBudget() : "not set")
                  .append("; Ethical/ESG investing preference: ")
                  .append(Boolean.TRUE.equals(p.getEthicalInvesting()) ? "YES" : "no").append('\n');
                sb.append("- Preferred sectors: ").append(preferred.isEmpty() ? "none specified" : String.join(", ", preferred))
                  .append("; Excluded sectors: ").append(excluded.isEmpty() ? "none" : String.join(", ", excluded)).append('\n');
            }

            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdWithHoldings(userId);
            if (portfolioOpt.isPresent() && portfolioOpt.get().getHoldings() != null
                    && !portfolioOpt.get().getHoldings().isEmpty()) {
                Portfolio portfolio = portfolioOpt.get();
                String holdings = portfolio.getHoldings().stream()
                        .map(h -> h.getSymbol() + " x" + h.getQuantity())
                        .collect(Collectors.joining(", "));
                sb.append("- Current holdings: ").append(holdings)
                  .append(" (use USER_PROFILE agent tools for live values)\n");
            } else {
                sb.append("- Current holdings: none\n");
            }

            String allocation = buildAllocationSummary(userId);
            if (!allocation.isEmpty()) {
                sb.append("- ").append(allocation).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not build profile context for {}: {}", userId, e.getMessage());
            return "USER PROFILE CONTEXT: unavailable (lookup failed).\n";
        }
    }

    /**
     * Per-holding allocation percentages + simple concentration assessment, computed from stored
     * values. Returns an empty string when there is no portfolio data to analyze.
     */
    @Transactional(readOnly = true)
    public String buildAllocationSummary(String userId) {
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findByUserIdWithHoldings(userId);
            if (portfolioOpt.isEmpty() || portfolioOpt.get().getHoldings() == null
                    || portfolioOpt.get().getHoldings().isEmpty()
                    || portfolioOpt.get().getTotalValue() == null
                    || portfolioOpt.get().getTotalValue().signum() <= 0) {
                return "";
            }
            Portfolio portfolio = portfolioOpt.get();
            StringBuilder sb = new StringBuilder("Portfolio allocation: ");
            double maxPct = 0;
            String maxSymbol = "";
            List<String> parts = new ArrayList<>();
            for (StockHolding h : portfolio.getHoldings()) {
                if (h.getValue() == null) {
                    continue;
                }
                double pct = h.getValue().doubleValue() / portfolio.getTotalValue().doubleValue() * 100.0;
                parts.add(String.format("%s %.1f%%", h.getSymbol(), pct));
                if (pct > maxPct) {
                    maxPct = pct;
                    maxSymbol = h.getSymbol();
                }
            }
            sb.append(String.join(", ", parts));
            if (maxPct > 30.0) {
                sb.append(String.format(". Concentration note: %s is %.1f%% of the portfolio (above the 30%% diversification guideline).",
                        maxSymbol, maxPct));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not build allocation summary for {}: {}", userId, e.getMessage());
            return "";
        }
    }
}
