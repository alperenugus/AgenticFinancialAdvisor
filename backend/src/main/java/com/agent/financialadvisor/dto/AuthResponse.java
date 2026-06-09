package com.agent.financialadvisor.dto;

import com.agent.financialadvisor.model.User;

import java.util.Map;

/**
 * Response for register/login: a JWT plus the public user fields. Shape matches what the frontend
 * already reads from GET /api/auth/me ({ id, email, name, pictureUrl }).
 */
public record AuthResponse(String token, Map<String, Object> user) {

    public static AuthResponse of(String token, User user) {
        Map<String, Object> u = new java.util.HashMap<>();
        u.put("id", user.getId());
        u.put("email", user.getEmail());
        u.put("name", user.getName());
        u.put("pictureUrl", user.getPictureUrl());
        return new AuthResponse(token, u);
    }
}
