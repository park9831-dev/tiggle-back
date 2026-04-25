package com.tiggle.autotrading.repository;

import com.tiggle.autotrading.model.KiwoomCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KiwoomCredentialRepository extends JpaRepository<KiwoomCredential, Long> {
    Optional<KiwoomCredential> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
