package com.tiggle.autotrading.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.tiggle.autotrading.dto.LogRequest;
import com.tiggle.autotrading.dto.LogResponse;
import com.tiggle.autotrading.model.UserLog;
import com.tiggle.autotrading.service.UserLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User log endpoints.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class UserLogController {

    private static final Logger log = LoggerFactory.getLogger(UserLogController.class);
    private final UserLogService userLogService;

    public UserLogController(UserLogService userLogService) {
        this.userLogService = userLogService;
    }

    @PostMapping
    public ResponseEntity<?> saveLog(@RequestBody LogRequest request, HttpServletRequest httpRequest) {
        try {
            String accessIp = request.accessIp() != null ? request.accessIp() : getClientIp(httpRequest);
            UserLog savedLog = userLogService.saveLog(
                    request.userId(),
                    accessIp,
                    request.accessDevice(),
                    request.logType(),
                    request.log()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(LogResponse.from(savedLog));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<LogResponse>> getAllLogs() {
        List<LogResponse> logs = userLogService.getAllLogs().stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] all logs count={}", logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/type/{logType}")
    public ResponseEntity<List<LogResponse>> getLogsByType(@PathVariable String logType) {
        List<LogResponse> logs = userLogService.getLogsByType(logType).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] type={} count={}", logType, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/range")
    public ResponseEntity<List<LogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<LogResponse> logs = userLogService.getLogsByDateRange(start, end).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] range start={} end={} count={}", start, end, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/ip/{accessIp}")
    public ResponseEntity<List<LogResponse>> getLogsByIp(@PathVariable String accessIp) {
        List<LogResponse> logs = userLogService.getLogsByIp(accessIp).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] ip={} count={}", accessIp, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/type/{logType}/range")
    public ResponseEntity<List<LogResponse>> getLogsByTypeAndDateRange(
            @PathVariable String logType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<LogResponse> logs = userLogService.getLogsByTypeAndDateRange(logType, start, end).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] type={} range=({}, {}) count={}", logType, start, end, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LogResponse>> getLogsByUserId(@PathVariable Long userId) {
        List<LogResponse> logs = userLogService.getLogsByUserId(userId).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] userId={} count={}", userId, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}/type/{logType}")
    public ResponseEntity<List<LogResponse>> getLogsByUserIdAndType(
            @PathVariable Long userId,
            @PathVariable String logType) {
        List<LogResponse> logs = userLogService.getLogsByUserIdAndType(userId, logType).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] userId={} type={} count={}", userId, logType, logs.size());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<LogResponse>> getLogsByUserIdAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<LogResponse> logs = userLogService.getLogsByUserIdAndDateRange(userId, start, end).stream()
                .map(LogResponse::from)
                .collect(Collectors.toList());
        log.info("[Log] userId={} range=({}, {}) count={}", userId, start, end, logs.size());
        return ResponseEntity.ok(logs);
    }

    private String getClientIp(HttpServletRequest request) {
        final String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        String ip = null;
        for (String header : headers) {
            String candidate = request.getHeader(header);
            if (candidate != null && !candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                ip = candidate;
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private record ErrorResponse(String message) {}
}
