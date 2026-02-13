package com.agent.financialadvisor.util;

import com.agent.financialadvisor.model.User;
import com.agent.financialadvisor.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtil {

    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }

    public static Optional<User> getCurrentUser(UserRepository userRepository) {
        return getCurrentUserEmail()
                .flatMap(userRepository::findByEmail);
    }

    public static String getCurrentUserId(UserRepository userRepository) {
        return getCurrentUser(userRepository)
                .map(user -> user.getId().toString())
                .orElse(null);
    }
}

