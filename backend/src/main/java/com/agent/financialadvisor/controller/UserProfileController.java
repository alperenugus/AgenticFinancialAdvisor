package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
// CORS handled by WebConfig - no need for @CrossOrigin here
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);
    private final UserProfileRepository userProfileRepository;

    public UserProfileController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Get user profile
     * GET /api/profile/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId) {
        try {
            return userProfileRepository.findByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create user profile
     * POST /api/profile
     */
    @PostMapping
    public ResponseEntity<UserProfile> createUserProfile(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Check if profile already exists
            if (userProfileRepository.findByUserId(userId).isPresent()) {
                return ResponseEntity.badRequest().build();
            }

            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            
            // Set risk tolerance
            if (request.containsKey("riskTolerance")) {
                try {
                    profile.setRiskTolerance(UserProfile.RiskTolerance.valueOf(
                            ((String) request.get("riskTolerance")).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    profile.setRiskTolerance(UserProfile.RiskTolerance.MODERATE);
                }
            }

            // Set investment horizon
            if (request.containsKey("horizon")) {
                try {
                    profile.setHorizon(UserProfile.InvestmentHorizon.valueOf(
                            ((String) request.get("horizon")).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    profile.setHorizon(UserProfile.InvestmentHorizon.MEDIUM);
                }
            }

            // Set goals
            if (request.containsKey("goals")) {
                @SuppressWarnings("unchecked")
                List<String> goals = (List<String>) request.get("goals");
                profile.setGoals(goals);
            }

            // Set budget
            if (request.containsKey("budget")) {
                Object budgetObj = request.get("budget");
                if (budgetObj instanceof Number) {
                    profile.setBudget(BigDecimal.valueOf(((Number) budgetObj).doubleValue()));
                } else if (budgetObj instanceof String) {
                    profile.setBudget(new BigDecimal((String) budgetObj));
                }
            }

            // Set preferred sectors
            if (request.containsKey("preferredSectors")) {
                @SuppressWarnings("unchecked")
                List<String> sectors = (List<String>) request.get("preferredSectors");
                profile.setPreferredSectors(sectors);
            }

            // Set excluded sectors
            if (request.containsKey("excludedSectors")) {
                @SuppressWarnings("unchecked")
                List<String> sectors = (List<String>) request.get("excludedSectors");
                profile.setExcludedSectors(sectors);
            }

            // Set ethical investing
            if (request.containsKey("ethicalInvesting")) {
                profile.setEthicalInvesting((Boolean) request.get("ethicalInvesting"));
            }

            UserProfile saved = userProfileRepository.save(profile);
            log.info("Created user profile for userId={}", userId);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update user profile
     * PUT /api/profile/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            UserProfile profile = profileOpt.get();

            // Update risk tolerance
            if (request.containsKey("riskTolerance")) {
                try {
                    profile.setRiskTolerance(UserProfile.RiskTolerance.valueOf(
                            ((String) request.get("riskTolerance")).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Invalid value, skip
                }
            }

            // Update investment horizon
            if (request.containsKey("horizon")) {
                try {
                    profile.setHorizon(UserProfile.InvestmentHorizon.valueOf(
                            ((String) request.get("horizon")).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Invalid value, skip
                }
            }

            // Update goals
            if (request.containsKey("goals")) {
                @SuppressWarnings("unchecked")
                List<String> goals = (List<String>) request.get("goals");
                profile.setGoals(goals);
            }

            // Update budget
            if (request.containsKey("budget")) {
                Object budgetObj = request.get("budget");
                if (budgetObj instanceof Number) {
                    profile.setBudget(BigDecimal.valueOf(((Number) budgetObj).doubleValue()));
                } else if (budgetObj instanceof String) {
                    profile.setBudget(new BigDecimal((String) budgetObj));
                }
            }

            // Update preferred sectors
            if (request.containsKey("preferredSectors")) {
                @SuppressWarnings("unchecked")
                List<String> sectors = (List<String>) request.get("preferredSectors");
                profile.setPreferredSectors(sectors);
            }

            // Update excluded sectors
            if (request.containsKey("excludedSectors")) {
                @SuppressWarnings("unchecked")
                List<String> sectors = (List<String>) request.get("excludedSectors");
                profile.setExcludedSectors(sectors);
            }

            // Update ethical investing
            if (request.containsKey("ethicalInvesting")) {
                profile.setEthicalInvesting((Boolean) request.get("ethicalInvesting"));
            }

            UserProfile updated = userProfileRepository.save(profile);
            log.info("Updated user profile for userId={}", userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

