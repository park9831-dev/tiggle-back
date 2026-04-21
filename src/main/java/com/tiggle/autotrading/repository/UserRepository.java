package com.tiggle.autotrading.repository;

import com.tiggle.autotrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByUsedBool(Short usedBool);

    long countByLockDateIsNotNull();

    long countByCreateAtAfter(LocalDateTime dateTime);

    long countByCreateAtBetween(LocalDateTime start, LocalDateTime end);

    long countByUserType(String userType);
}
