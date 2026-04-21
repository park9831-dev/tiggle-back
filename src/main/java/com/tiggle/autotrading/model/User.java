package com.tiggle.autotrading.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_info")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 128)
    private String password;

    @Column(name = "salt", nullable = false, length = 32)
    private String salt;

    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;

    @Column(name = "user_type", length = 30)
    private String userType;

    @Column(name = "mobile", length = 11)
    private String mobile;

    @Column(name = "used_bool", nullable = false)
    private Short usedBool = 1;

    @Column(name = "recent_ip", length = 100)
    private String recentIp;

    @Column(name = "fail_count")
    private Integer failCount = 0;

    @Column(name = "lock_date")
    private LocalDateTime lockDate;

    @Column(name = "previous_password", length = 128)
    private String previousPassword;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User(String email, String password, String salt, String userName, String userType) {
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.userName = userName;
        this.userType = userType;
    }

    public void incrementFailCount() {
        this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
    }

    public void resetFailCount() {
        this.failCount = 0;
        this.lockDate = null;
    }

    public void lockAccount() {
        this.lockDate = LocalDateTime.now();
    }

    public boolean isLocked() {
        return this.lockDate != null;
    }

    public void updateRecentIp(String ipAddress) {
        this.recentIp = ipAddress;
    }

    public void changePassword(String newPassword, String newSalt) {
        this.previousPassword = this.password;
        this.password = newPassword;
        this.salt = newSalt;
    }

    public void changePassword(String newPassword) {
        changePassword(newPassword, "");
    }

    // BCrypt 기반 authority 파생 (ADMIN=1, 그 외=2)
    public Integer getAuthority() {
        return "ADMIN".equals(userType) ? 1 : 2;
    }

    // email 필드의 alias (기존 코드 호환)
    public String getEmailId() {
        return email;
    }
}
