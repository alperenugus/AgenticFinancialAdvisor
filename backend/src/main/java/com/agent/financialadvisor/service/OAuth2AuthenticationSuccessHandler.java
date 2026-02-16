package com.agent.financialadvisor.service;

import com.agent.financialadvisor.model.User;
import com.agent.financialadvisor.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${CORS_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String corsOrigins;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String email = (String) attributes.get("email");
            
            log.info("OAuth2 authentication successful for email: {}", email);

            if (email == null || email.trim().isEmpty()) {
                log.error("Email is null or empty in OAuth2 attributes: {}", attributes);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Email not found in OAuth2 response");
                return;
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // User should have been created by CustomOAuth2UserService, but create if missing
                        log.warn("User not found in database after OAuth2 authentication, creating now. Email: {}", email);
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName((String) attributes.get("name"));
                        newUser.setGoogleId((String) attributes.get("sub"));
                        newUser.setPictureUrl((String) attributes.get("picture"));
                        return userRepository.save(newUser);
                    });

            log.info("User found/created: {} (ID: {})", user.getEmail(), user.getId());

            String token = jwtService.generateToken(user.getEmail(), user.getId());
            String frontendUrl = corsOrigins.split(",")[0].trim(); // Use first origin as redirect target

            // Put token in the URL fragment so it is not sent to servers/proxies as query params.
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String redirectUrl = frontendUrl + "/auth/callback#token=" + encodedToken;
            log.info("Redirecting to frontend callback for user {}", user.getEmail());
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            log.error("Error in OAuth2 authentication success handler", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error: " + e.getMessage());
        }
    }
}

