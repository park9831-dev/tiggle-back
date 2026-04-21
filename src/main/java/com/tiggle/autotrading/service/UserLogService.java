package com.tiggle.autotrading.service;

import com.tiggle.autotrading.model.UserLog;
import com.tiggle.autotrading.repository.UserLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserLogService {

    private final UserLogRepository userLogRepository;

    public UserLogService(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    @Transactional
    public UserLog saveLog(Long userId, String accessIp, String accessDevice, String logType, String log) {
        if (userId == null) throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        return userLogRepository.save(new UserLog(userId, accessIp, accessDevice, logType, log));
    }

    @Transactional
    public UserLog saveLog(Long userId, String accessIp, String accessDevice, String logType) {
        return saveLog(userId, accessIp, accessDevice, logType, null);
    }

    @Transactional
    public UserLog saveLog(Long userId, String accessIp, String logType) {
        return saveLog(userId, accessIp, null, logType, null);
    }

    public List<UserLog> getAllLogs() {
        return userLogRepository.findAll();
    }

    public List<UserLog> getLogsByType(String logType) {
        return userLogRepository.findByLogType(logType);
    }

    public List<UserLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return userLogRepository.findByCreateAtBetween(start, end);
    }

    public List<UserLog> getLogsByIp(String accessIp) {
        return userLogRepository.findByAccessIp(accessIp);
    }

    public List<UserLog> getLogsByTypeAndDateRange(String logType, LocalDateTime start, LocalDateTime end) {
        return userLogRepository.findByLogTypeAndCreateAtBetween(logType, start, end);
    }

    public List<UserLog> getLogsByUserId(Long userId) {
        return userLogRepository.findByUserId(userId);
    }

    public List<UserLog> getLogsByUserIdAndType(Long userId, String logType) {
        return userLogRepository.findByUserIdAndLogType(userId, logType);
    }

    public List<UserLog> getLogsByUserIdAndDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return userLogRepository.findByUserIdAndCreateAtBetween(userId, start, end);
    }
}
