package com.tiggle.autotrading.service;

import com.tiggle.autotrading.model.RefreshToken;
import com.tiggle.autotrading.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for issuing, rotating, validating, and deleting refresh tokens.
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-days:7}") long refreshTokenExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /**
     * Creates a new refresh token or updates the existing token for the user.
     */
    @Transactional
    public RefreshToken createOrUpdateToken(Long userId, String ipAddress) {
        String token = generateToken();
        LocalDateTime expireDate = LocalDateTime.now().plusDays(refreshTokenExpirationDays);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);

        if (existingToken.isPresent()) {
            // Update existing token row for the user.
            RefreshToken refreshToken = existingToken.get();
            refreshToken.updateToken(token, expireDate, ipAddress);
            return refreshTokenRepository.save(refreshToken);
        } else {
            // Create initial token row for the user.
            RefreshToken refreshToken = new RefreshToken(userId, token, expireDate);
            refreshToken.setRecentIp(ipAddress);
            return refreshTokenRepository.save(refreshToken);
        }
    }

    /**
     * Returns token entity only when it exists and is not expired.
     */
    public Optional<RefreshToken> validateAndGetToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        // Expired tokens are treated as invalid.
        if (refreshToken.isExpired()) {
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    /**
     * Rotates refresh token value and expiration while keeping same row.
     */
    @Transactional
    public RefreshToken rotateToken(RefreshToken oldToken, String ipAddress) {
        String newToken = generateToken();
        LocalDateTime newExpireDate = LocalDateTime.now().plusDays(refreshTokenExpirationDays);

        oldToken.updateToken(newToken, newExpireDate, ipAddress);
        return refreshTokenRepository.save(oldToken);
    }

    /**
     * Deletes refresh token by user id.
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Deletes refresh token by token string.
     */
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Updates recent activity metadata for the token.
     */
    @Transactional
    public void updateRecentActive(String token, String ipAddress) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.updateRecentActive(ipAddress);
                    refreshTokenRepository.save(rt);
                });
    }

    /**
     * Generates a cryptographically strong URL-safe token value.
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

