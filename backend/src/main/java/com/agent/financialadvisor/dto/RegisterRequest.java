package com.agent.financialadvisor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for POST /api/auth/register (email/password sign-up). */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 100) String password
) {}
