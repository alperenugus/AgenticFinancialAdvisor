package com.agent.financialadvisor.service;

import com.agent.financialadvisor.model.User;
import com.agent.financialadvisor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            log.info("Loading OAuth2 user from Google");
            OAuth2User oAuth2User = super.loadUser(userRequest);
            log.info("OAuth2 user loaded successfully, attributes: {}", oAuth2User.getAttributes().keySet());
            return processOAuth2User(oAuth2User);
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2 authentication error", e);
            throw e;
        } catch (Exception e) {
            log.error("Error loading OAuth2 user", e);
            throw new OAuth2AuthenticationException("Failed to load user from Google");
        }
    }

    @Transactional
    private OAuth2User processOAuth2User(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String googleId = (String) attributes.get("sub");

        log.info("Processing OAuth2 user - Email: {}, Name: {}, GoogleId: {}", email, name, googleId);

        if (email == null || email.trim().isEmpty()) {
            log.error("Email is null or empty in OAuth2 attributes: {}", attributes);
            throw new OAuth2AuthenticationException("Email not found in OAuth2 response");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new user for email: {}", email);
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setGoogleId(googleId);
                    newUser.setPictureUrl(picture);
                    User saved = userRepository.save(newUser);
                    log.info("New user created with ID: {}", saved.getId());
                    return saved;
                });

        // Update user info if needed
        boolean updated = false;
        if (user.getGoogleId() == null && googleId != null) {
            user.setGoogleId(googleId);
            updated = true;
        }
        if (user.getPictureUrl() == null && picture != null) {
            user.setPictureUrl(picture);
            updated = true;
        }
        if (user.getName() == null && name != null) {
            user.setName(name);
            updated = true;
        }
        
        if (updated) {
            userRepository.save(user);
            log.info("User updated: {}", user.getEmail());
        }

        return oAuth2User;
    }
}

