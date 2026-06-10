package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.model.User;
import com.agent.financialadvisor.repository.UserRepository;
import com.agent.financialadvisor.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void register_createsUser_andReturnsToken() throws Exception {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generateToken(eq("new@example.com"), anyLong())).thenReturn("jwt-token");

        var body = Map.of("email", "new@example.com", "name", "New User", "password", "supersecret");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("new@example.com"));
    }

    @Test
    void register_rejectsDuplicateEmail_with409() throws Exception {
        when(userRepository.existsByEmail("dupe@example.com")).thenReturn(true);

        var body = Map.of("email", "dupe@example.com", "name", "Dupe", "password", "supersecret");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsWeakPassword_with400() throws Exception {
        var body = Map.of("email", "new@example.com", "name", "New", "password", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_succeeds_withCorrectPassword() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("supersecret", "hashed")).thenReturn(true);
        when(jwtService.generateToken(eq("alice@example.com"), anyLong())).thenReturn("jwt-token");

        var body = Map.of("email", "alice@example.com", "password", "supersecret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_rejectsWrongPassword_with401() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("hashed"))).thenReturn(false);

        var body = Map.of("email", "alice@example.com", "password", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_rejectsGoogleOnlyAccount_with401() throws Exception {
        User googleUser = new User();
        googleUser.setEmail("google@example.com");
        googleUser.setPasswordHash(null); // Google-only, no local password
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(googleUser));

        var body = Map.of("email", "google@example.com", "password", "ansomething");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
