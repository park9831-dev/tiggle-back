package com.tiggle.autotrading.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthorized access and returns a standardized 401 JSON response.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access - URI: {}, error: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Use error metadata set by JwtAuthenticationFilter when available.
        String errorCode = (String) request.getAttribute("jwt_error_code");
        String errorMessage = (String) request.getAttribute("jwt_error_message");

        if (errorCode == null) {
            errorCode = "AUTHENTICATION_REQUIRED";
            errorMessage = "Authentication is required.";
        }

        response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"code\":\"%s\"}",
                errorMessage, errorCode
        ));
    }
}
