package com.agent.financialadvisor.service.agents;

import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileAgentTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileAgent userProfileAgent;

    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new UserProfile();
        testProfile.setId(1L);
        testProfile.setUserId("test-user");
        testProfile.setRiskTolerance(UserProfile.RiskTolerance.MODERATE);
        testProfile.setHorizon(UserProfile.InvestmentHorizon.MEDIUM);
        testProfile.setGoals(Arrays.asList("GROWTH", "RETIREMENT"));
        testProfile.setBudget(new BigDecimal("10000"));
        testProfile.setPreferredSectors(Arrays.asList("Technology", "Healthcare"));
        testProfile.setEthicalInvesting(true);
    }

    @Test
    void testGetUserProfile_WhenProfileExists() {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        // When
        String result = userProfileAgent.getUserProfile("test-user");

        // Then
        assertThat(result).contains("test-user");
        assertThat(result).contains("MODERATE");
        assertThat(result).contains("MEDIUM");
        assertThat(result).contains("\"exists\": true");
        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetUserProfile_WhenProfileDoesNotExist() {
        // Given
        when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When
        String result = userProfileAgent.getUserProfile("non-existent");

        // Then
        assertThat(result).contains("non-existent");
        assertThat(result).contains("\"exists\": false");
        verify(userProfileRepository, times(1)).findByUserId("non-existent");
    }

    @Test
    void testUpdateRiskTolerance_Success() {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When
        String result = userProfileAgent.updateRiskTolerance("test-user", "AGGRESSIVE");

        // Then
        assertThat(result).contains("AGGRESSIVE");
        assertThat(result).contains("updated successfully");
        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void testUpdateRiskTolerance_InvalidValue() {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        // When
        String result = userProfileAgent.updateRiskTolerance("test-user", "INVALID");

        // Then
        assertThat(result).contains("error");
        assertThat(result).contains("Invalid risk tolerance");
        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void testUpdateRiskTolerance_ProfileNotFound() {
        // Given
        when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When
        String result = userProfileAgent.updateRiskTolerance("non-existent", "MODERATE");

        // Then
        assertThat(result).contains("error");
        assertThat(result).contains("not found");
    }

    @Test
    void testGetInvestmentGoals_WhenProfileExists() {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        // When
        String result = userProfileAgent.getInvestmentGoals("test-user");

        // Then
        assertThat(result).contains("test-user");
        assertThat(result).contains("GROWTH");
        assertThat(result).contains("RETIREMENT");
        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetInvestmentGoals_WhenProfileDoesNotExist() {
        // Given
        when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When
        String result = userProfileAgent.getInvestmentGoals("non-existent");

        // Then
        assertThat(result).contains("non-existent");
        assertThat(result).contains("\"goals\": []");
    }
}

