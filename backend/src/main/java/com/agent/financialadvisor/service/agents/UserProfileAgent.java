package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.UserProfileRepository;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserProfileAgent {

    private static final Logger log = LoggerFactory.getLogger(UserProfileAgent.class);
    private final UserProfileRepository userProfileRepository;

    public UserProfileAgent(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Tool("Get user's investment profile including risk tolerance, investment horizon, goals, and preferences. " +
          "Use this to understand the user's investment preferences before making recommendations. " +
          "Requires: userId (string). Returns user profile information.")
    public String getUserProfile(String userId) {
        log.info("🔵 getUserProfile CALLED with userId={}", userId);
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return String.format(
                    "{\"userId\": \"%s\", \"exists\": false, \"message\": \"User profile not found. User needs to create a profile first.\"}",
                    userId
                );
            }
            
            UserProfile profile = profileOpt.get();
            return String.format(
                "{\"userId\": \"%s\", \"exists\": true, \"riskTolerance\": \"%s\", \"horizon\": \"%s\", " +
                "\"goals\": %s, \"budget\": %s, \"preferredSectors\": %s, \"excludedSectors\": %s, " +
                "\"ethicalInvesting\": %s, \"message\": \"User profile retrieved\"}",
                profile.getUserId(),
                profile.getRiskTolerance(),
                profile.getHorizon(),
                profile.getGoals() != null ? profile.getGoals().toString() : "[]",
                profile.getBudget() != null ? profile.getBudget().toString() : "null",
                profile.getPreferredSectors() != null ? profile.getPreferredSectors().toString() : "[]",
                profile.getExcludedSectors() != null ? profile.getExcludedSectors().toString() : "[]",
                profile.getEthicalInvesting()
            );
        } catch (Exception e) {
            log.error("Error getting user profile: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting user profile: %s\"}", e.getMessage());
        }
    }

    @Tool("Update user's risk tolerance level. Use this when the user wants to change their risk tolerance. " +
          "Requires: userId (string), riskTolerance (CONSERVATIVE, MODERATE, or AGGRESSIVE). " +
          "Returns updated profile.")
    public String updateRiskTolerance(String userId, String riskTolerance) {
        log.info("🔵 updateRiskTolerance CALLED with userId={}, riskTolerance={}", userId, riskTolerance);
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
            
            UserProfile.RiskTolerance tolerance;
            try {
                tolerance = UserProfile.RiskTolerance.valueOf(riskTolerance.toUpperCase());
            } catch (IllegalArgumentException e) {
                return String.format("{\"error\": \"Invalid risk tolerance: %s. Must be CONSERVATIVE, MODERATE, or AGGRESSIVE\"}", riskTolerance);
            }
            
            profile.setRiskTolerance(tolerance);
            userProfileRepository.save(profile);
            
            return String.format(
                "{\"userId\": \"%s\", \"riskTolerance\": \"%s\", \"message\": \"Risk tolerance updated successfully\"}",
                userId, tolerance
            );
        } catch (Exception e) {
            log.error("Error updating risk tolerance: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error updating risk tolerance: %s\"}", e.getMessage());
        }
    }

    @Tool("Get user's investment goals. Returns list of goals like RETIREMENT, GROWTH, INCOME. " +
          "Requires: userId (string).")
    public String getInvestmentGoals(String userId) {
        log.info("🔵 getInvestmentGoals CALLED with userId={}", userId);
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return String.format("{\"userId\": \"%s\", \"goals\": [], \"message\": \"User profile not found\"}", userId);
            }
            
            List<String> goals = profileOpt.get().getGoals();
            return String.format(
                "{\"userId\": \"%s\", \"goals\": %s, \"message\": \"Investment goals retrieved\"}",
                userId, goals != null ? goals.toString() : "[]"
            );
        } catch (Exception e) {
            log.error("Error getting investment goals: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting investment goals: %s\"}", e.getMessage());
        }
    }
}

