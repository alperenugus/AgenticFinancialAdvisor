package com.agent.financialadvisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-minimum-32-characters-long-xyz";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 86_400_000L);
    }

    @Test
    void generateToken_roundTrips_emailAndUserId() {
        String token = jwtService.generateToken("alice@example.com", 42L);

        assertEquals("alice@example.com", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertTrue(jwtService.validateToken(token, "alice@example.com"));
    }

    @Test
    void validateToken_rejects_wrongSubject() {
        String token = jwtService.generateToken("alice@example.com", 1L);
        assertFalse(jwtService.validateToken(token, "mallory@example.com"));
    }

    @Test
    void validateToken_rejects_expiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // already expired
        String token = jwtService.generateToken("alice@example.com", 1L);
        assertThrows(Exception.class, () -> jwtService.validateToken(token, "alice@example.com"));
    }

    @Test
    void extractEmail_rejects_tokenSignedWithDifferentSecret() {
        JwtService other = new JwtService();
        ReflectionTestUtils.setField(other, "secret", "a-completely-different-secret-key-also-32-chars+");
        ReflectionTestUtils.setField(other, "expiration", 86_400_000L);
        String forged = other.generateToken("alice@example.com", 1L);

        // Our service must not accept a token signed by a different key.
        assertThrows(Exception.class, () -> jwtService.extractEmail(forged));
    }

    @Test
    void validateSecret_rejects_blankShortAndPlaceholder() {
        JwtService svc = new JwtService();

        ReflectionTestUtils.setField(svc, "secret", "");
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateSecret"));

        ReflectionTestUtils.setField(svc, "secret", "too-short");
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateSecret"));

        ReflectionTestUtils.setField(svc, "secret",
                "your-256-bit-secret-key-change-this-in-production-minimum-32-characters");
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateSecret"));
    }
}
