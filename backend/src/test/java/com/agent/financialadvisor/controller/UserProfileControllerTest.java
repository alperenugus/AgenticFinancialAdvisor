package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserProfileController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileRepository userProfileRepository;

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
    }

    @Test
    void testGetUserProfile_Success() throws Exception {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        // When & Then
        mockMvc.perform(get("/api/profile/test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("test-user"))
                .andExpect(jsonPath("$.riskTolerance").value("MODERATE"));

        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetUserProfile_NotFound() throws Exception {
        // Given
        when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/profile/non-existent"))
                .andExpect(status().isNotFound());

        verify(userProfileRepository, times(1)).findByUserId("non-existent");
    }

    @Test
    void testCreateUserProfile_Success() throws Exception {
        // Given
        when(userProfileRepository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When & Then
        mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"new-user\",\"riskTolerance\":\"MODERATE\"}"))
                .andExpect(status().isOk());

        verify(userProfileRepository, times(1)).findByUserId("new-user");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_Success() throws Exception {
        // Given
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When & Then
        mockMvc.perform(put("/api/profile/test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskTolerance\":\"AGGRESSIVE\"}"))
                .andExpect(status().isOk());

        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_NotFound() throws Exception {
        // Given
        when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/profile/non-existent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskTolerance\":\"AGGRESSIVE\"}"))
                .andExpect(status().isNotFound());

        verify(userProfileRepository, times(1)).findByUserId("non-existent");
        verify(userProfileRepository, never()).save(any());
    }
}

