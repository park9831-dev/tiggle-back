package com.tiggle.autotrading.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates JWT access tokens.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final SecretKey secretKey;
    private final long expirationMs;


    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Creates a signed JWT with username and optional authority claim.
     * authority: 1=ADMIN, 2=OPERATOR, 3=USER
     */
    public String generateToken(String username, Integer authority) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("authority", authority)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Creates a signed JWT without authority claim.
     */
    public String generateToken(String username) {
        return generateToken(username, null);
    }

    /**
     * Extracts username(subject) from token.
     */
    public Optional<String> extractUsername(String token) {
        return parseToken(token).map(Claims::getSubject);
    }

    /**
     * Extracts numeric authority claim from token.
     * authority: 1=ADMIN, 2=OPERATOR, 3=USER
     */
    public Optional<Integer> extractAuthority(String token) {
        return parseToken(token).map(claims -> claims.get("authority", Integer.class));
    }

    /**
     * Returns true when token can be parsed and verified.
     */
    public boolean isValid(String token) {
        return parseToken(token).isPresent();
    }

    /**
     * Validates token and returns either parsed claims or failure metadata.
     *
     * @return ValidationResult validation outcome with claims or error info
     */
    public ValidationResult validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return ValidationResult.success(claims);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT expired - subject: {}, expiredAt: {}",
                    e.getClaims().getSubject(), e.getClaims().getExpiration());
            return ValidationResult.failure("TOKEN_EXPIRED", "Token has expired.");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT signature verification failed: {}", e.getMessage());
            return ValidationResult.failure("INVALID_TOKEN", "Invalid token signature.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return ValidationResult.failure("INVALID_TOKEN", "Malformed token format.");
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return ValidationResult.failure("INVALID_TOKEN", "Unsupported token type.");
        } catch (JwtException e) {
            log.warn("JWT validation failed [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            return ValidationResult.failure("INVALID_TOKEN", "Token validation failed.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
            return ValidationResult.failure("INVALID_TOKEN", "Token is empty.");
        }
    }

    /**
     * Result object for token validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Claims claims;
        private final String errorCode;
        private final String errorMessage;

        private ValidationResult(boolean valid, Claims claims, String errorCode, String errorMessage) {
            this.valid = valid;
            this.claims = claims;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success(Claims claims) {
            return new ValidationResult(true, claims, null, null);
        }

        public static ValidationResult failure(String errorCode, String errorMessage) {
            return new ValidationResult(false, null, errorCode, errorMessage);
        }

        public boolean isValid() { return valid; }
        public Claims getClaims() { return claims; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
    }

    private Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT expired - subject: {}, expiredAt: {}",
                    e.getClaims().getSubject(), e.getClaims().getExpiration());
            return Optional.empty();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT signature verification failed: {}", e.getMessage());
            return Optional.empty();
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return Optional.empty();
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException e) {
            log.warn("JWT validation failed [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

