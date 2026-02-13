package com.agent.financialadvisor.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS Filter to handle CORS headers for all requests including SockJS endpoints
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Value("${CORS_ORIGINS:*}")
    private String corsOrigins;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Get origin from request
        String origin = request.getHeader("Origin");
        
        // Determine allowed origins
        String[] allowedOrigins = corsOrigins.split(",");
        boolean allowOrigin = false;
        String allowedOrigin = null;

        if (corsOrigins.equals("*") || corsOrigins.contains("*")) {
            // Wildcard - allow any origin
            allowOrigin = true;
            allowedOrigin = origin != null ? origin : "*";
        } else {
            // Check if origin is in allowed list
            for (String allowed : allowedOrigins) {
                if (origin != null && origin.trim().equals(allowed.trim())) {
                    allowOrigin = true;
                    allowedOrigin = origin;
                    break;
                }
            }
        }

        // Set CORS headers
        if (allowOrigin && allowedOrigin != null) {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", 
                    corsOrigins.equals("*") || corsOrigins.contains("*") ? "false" : "true");
            response.setHeader("Access-Control-Max-Age", "3600");
        }

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}

