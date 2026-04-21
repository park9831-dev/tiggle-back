package com.tiggle.autotrading.dto;

import com.tiggle.autotrading.model.UserLog;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        Long userId,
        LocalDateTime createAt,
        String accessIp,
        String accessDevice,
        String logType,
        String log
) {
    public static LogResponse from(UserLog userLog) {
        return new LogResponse(
                userLog.getId(),
                userLog.getUserId(),
                userLog.getCreateAt(),
                userLog.getAccessIp(),
                userLog.getAccessDevice(),
                userLog.getLogType(),
                userLog.getLog()
        );
    }
}
