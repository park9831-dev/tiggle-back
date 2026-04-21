package com.tiggle.autotrading.dto;

/**
 * Authentication response DTO with optional JWT token.
 */
public record AuthResponse(String message, String token) {

    /**
     * Constructor for message-only responses.
     */
    public AuthResponse(String message) {
        this(message, null);
    }
}
