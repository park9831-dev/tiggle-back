package com.tiggle.autotrading.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_access_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "access_type", length = 32)
    private String accessType;

    @Column(name = "ip", length = 39)
    private String ip;

    @Column(name = "log", columnDefinition = "TEXT")
    private String log;

    @Column(name = "create_at", updatable = false)
    private LocalDateTime createAt;

    @PrePersist
    protected void onCreate() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }

    public UserAccessLog(Long userId, String accessType, String ip, String log) {
        this.userId = userId;
        this.accessType = accessType;
        this.ip = ip;
        this.log = log;
    }
}
