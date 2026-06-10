package com.agent.financialadvisor.controller;

import com.agent.financialadvisor.dto.AuthResponse;
import com.agent.financialadvisor.dto.LoginRequest;
import com.agent.financialadvisor.dto.RegisterRequest;
import com.agent.financialadvisor.model.User;
import com.agent.financialadvisor.repository.UserRepository;
import com.agent.financialadvisor.service.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a local (email/password) account and return a JWT.
     * POST /api/auth/register  { email, name, password }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("An account with this email already exists."));
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider("LOCAL");
        user = userRepository.save(user);

        log.info("Registered new local user: {} (ID: {})", email, user.getId());
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.of(token, user));
    }

    /**
     * Authenticate a local (email/password) account and return a JWT.
     * POST /api/auth/login  { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Same generic message whether the email is unknown, the account is Google-only, or the
        // password is wrong — avoids leaking which emails are registered.
        if (userOpt.isEmpty()
                || userOpt.get().getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Invalid email or password."));
        }

        User user = userOpt.get();
        log.info("Local login succeeded for: {} (ID: {})", email, user.getId());
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return ResponseEntity.ok(AuthResponse.of(token, user));
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", message);
        return body;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("pictureUrl", user.getPictureUrl());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/login/google")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("url", "/oauth2/authorization/google");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        try {
            String email = jwtService.extractEmail(token);

            if (jwtService.validateToken(token, email)) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("email", user.getEmail());
                    response.put("userId", user.getId());
                    response.put("name", user.getName());
                    return ResponseEntity.ok(response);
                }
            }
        } catch (Exception e) {
            // Token invalid
        }

        return ResponseEntity.status(401).build();
    }
}

