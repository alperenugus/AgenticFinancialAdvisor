package com.agent.financialadvisor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload for POST /api/auth/login (email/password sign-in). */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
