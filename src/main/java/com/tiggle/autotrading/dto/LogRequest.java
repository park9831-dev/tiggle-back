package com.tiggle.autotrading.dto;

public record LogRequest(
        Long userId,
        String accessIp,
        String accessDevice,
        String logType,
        String log
) {}
