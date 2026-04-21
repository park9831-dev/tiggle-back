package com.tiggle.autotrading.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_refreshtoken_info")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "token", length = 256)
    private String token;

    @Column(name = "active_dt")
    private LocalDateTime activeDt;

    @Column(name = "expire_dt")
    private LocalDateTime expireDt;

    @Column(name = "recent_ip", length = 39)
    private String recentIp;

    public RefreshToken(Long userId, String token, LocalDateTime expireDt) {
        this.userId = userId;
        this.token = token;
        this.expireDt = expireDt;
        this.activeDt = LocalDateTime.now();
    }

    public void updateToken(String newToken, LocalDateTime newExpireDt, String ipAddress) {
        this.token = newToken;
        this.expireDt = newExpireDt;
        this.activeDt = LocalDateTime.now();
        this.recentIp = ipAddress;
    }

    public boolean isExpired() {
        return this.expireDt != null && LocalDateTime.now().isAfter(this.expireDt);
    }

    public void updateRecentActive(String ipAddress) {
        this.activeDt = LocalDateTime.now();
        this.recentIp = ipAddress;
    }
}
