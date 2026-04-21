package com.tiggle.autotrading.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Extracts and validates JWT tokens from the Authorization header.
 * On success, it populates Spring Security Authentication in the context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String requestUri = request.getRequestURI();

        // Log each request with the existence of Authorization header for debugging.
        log.info("[JWT Filter] Request URI: {}, Authorization header exists: {}",
                requestUri, authHeader != null);
        
        if (authHeader != null) {
            log.info("[JWT Filter] Authorization header preview: {}... (length: {})",
                    authHeader.length() > 20 ? authHeader.substring(0, 20) : authHeader, 
                    authHeader.length());
        }

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            // Reject blank bearer token immediately.
            if (token == null || token.trim().isEmpty()) {
                log.warn("[JWT Filter] Empty JWT token received. URI: {}", requestUri);
                sendUnauthorizedResponse(response, "INVALID_TOKEN", "Token is empty.");
                return;
            }

            log.info("[JWT Filter] Starting token validation. URI: {}, tokenLength: {}", requestUri, token.length());

            // Validate token and short-circuit on failure.
            JwtService.ValidationResult result = jwtService.validateToken(token);

            if (!result.isValid()) {
                log.warn("[JWT Filter] JWT validation failed. URI: {}, code: {}, message: {}",
                        requestUri, result.getErrorCode(), result.getErrorMessage());
                sendUnauthorizedResponse(response, result.getErrorCode(), result.getErrorMessage());
                return;
            }

            // Build authentication details from validated claims.
            String username = result.getClaims().getSubject();
            Integer authority = result.getClaims().get("authority", Integer.class);
            log.info("[JWT Filter] JWT validated. URI: {}, username: {}, authority: {}",
                    requestUri, username, authority);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // authority mapping: 1=ADMIN, 2=OPERATOR, 3=USER
                List<SimpleGrantedAuthority> authorities = mapAuthority(authority);

                log.info("[JWT Filter] Authentication set. username: {}, authority: {}, roles: {}",
                        username, authority, authorities);

                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.info("[JWT Filter] SecurityContext already contains authentication. URI: {}", requestUri);
            }
        } else {
            log.warn("[JWT Filter] Authorization header is missing or not Bearer. URI: {}, reason: {}",
                    requestUri, authHeader != null ? "Header is present but does not start with Bearer" : "Header is missing");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a standardized 401 Unauthorized JSON response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response,
                                          String errorCode,
                                          String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"code\":\"%s\"}", errorMessage, errorCode));
    }

    /**
     * Maps numeric authority values to Spring Security roles.
     * 1=ROLE_ADMIN, 2=ROLE_OPERATOR, 3=ROLE_USER.
     */
    private List<SimpleGrantedAuthority> mapAuthority(Integer authority) {
        if (authority == null) {
            return Collections.emptyList();
        }

        return switch (authority) {
            case 1 -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case 2 -> List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"));
            case 3 -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
            default -> Collections.emptyList();
        };
    }
}

