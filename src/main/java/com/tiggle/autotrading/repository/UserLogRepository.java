package com.tiggle.autotrading.repository;

import com.tiggle.autotrading.model.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    List<UserLog> findByLogType(String logType);

    List<UserLog> findByCreateAtBetween(LocalDateTime start, LocalDateTime end);

    List<UserLog> findByAccessIp(String accessIp);

    List<UserLog> findByLogTypeAndCreateAtBetween(String logType, LocalDateTime start, LocalDateTime end);

    List<UserLog> findByUserId(Long userId);

    List<UserLog> findByUserIdAndLogType(Long userId, String logType);

    List<UserLog> findByUserIdAndCreateAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
