package com.tiggle.autotrading.dto;

/**
 * Response DTO containing access/refresh token pair.
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,         // Access token expiration (milliseconds)
    String message
) {
    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn, "Token issued successfully");
    }

    public TokenResponse(String accessToken, String refreshToken, long expiresIn, String message) {
        this(accessToken, refreshToken, "Bearer", expiresIn, message);
    }
}

