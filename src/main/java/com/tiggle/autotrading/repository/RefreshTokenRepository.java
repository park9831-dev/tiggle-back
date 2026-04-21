package com.tiggle.autotrading.repository;

import com.tiggle.autotrading.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for refresh token persistence and lookup.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds refresh token by token value.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds refresh token by user id.
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * Deletes refresh token by user id.
     */
    void deleteByUserId(Long userId);

    /**
     * Checks whether the token value already exists.
     */
    boolean existsByToken(String token);
}

