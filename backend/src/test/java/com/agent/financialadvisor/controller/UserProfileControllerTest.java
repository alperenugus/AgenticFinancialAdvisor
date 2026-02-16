package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.UserProfile;
import com.agent.financialadvisor.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserProfileController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileRepository userProfileRepository;

    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("test-user", "n/a");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        testProfile = new UserProfile();
        testProfile.setId(1L);
        testProfile.setUserId("test-user");
        testProfile.setRiskTolerance(UserProfile.RiskTolerance.MODERATE);
        testProfile.setHorizon(UserProfile.InvestmentHorizon.MEDIUM);
        testProfile.setGoals(Arrays.asList("GROWTH", "RETIREMENT"));
        testProfile.setBudget(new BigDecimal("10000"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUserProfile_Success() throws Exception {
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("test-user"))
                .andExpect(jsonPath("$.riskTolerance").value("MODERATE"));

        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testGetUserProfile_NotFound() throws Exception {
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isNotFound());

        verify(userProfileRepository, times(1)).findByUserId("test-user");
    }

    @Test
    void testCreateUserProfile_Success() throws Exception {
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskTolerance\":\"MODERATE\"}"))
                .andExpect(status().isOk());

        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_Success() throws Exception {
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskTolerance\":\"AGGRESSIVE\"}"))
                .andExpect(status().isOk());

        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_NotFound() throws Exception {
        when(userProfileRepository.findByUserId("test-user")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskTolerance\":\"AGGRESSIVE\"}"))
                .andExpect(status().isNotFound());

        verify(userProfileRepository, times(1)).findByUserId("test-user");
        verify(userProfileRepository, never()).save(any());
    }
}

