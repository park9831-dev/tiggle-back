package com.tiggle.autotrading.dto;

/**
 * Request DTO for changing a user's password.
 */
public record PasswordChangeRequest(
    String newPassword      // New plain password to be validated and encoded
) {}

