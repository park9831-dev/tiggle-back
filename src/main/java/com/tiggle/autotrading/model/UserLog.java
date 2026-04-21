package com.tiggle.autotrading.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "access_ip", length = 100)
    private String accessIp;

    @Column(name = "access_device", length = 500)
    private String accessDevice;

    @Column(name = "log_type", length = 20)
    private String logType;

    @Column(name = "log", columnDefinition = "TEXT")
    private String log;

    @Column(name = "user_id")
    private Long userId;

    @PrePersist
    protected void onCreate() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }

    public UserLog(Long userId, String accessIp, String accessDevice, String logType, String log) {
        this.userId = userId;
        this.accessIp = accessIp;
        this.accessDevice = accessDevice;
        this.logType = logType;
        this.log = log;
    }

    public UserLog(Long userId, String accessIp, String accessDevice, String logType) {
        this(userId, accessIp, accessDevice, logType, null);
    }

    public UserLog(Long userId, String accessIp, String logType) {
        this(userId, accessIp, null, logType, null);
    }
}
