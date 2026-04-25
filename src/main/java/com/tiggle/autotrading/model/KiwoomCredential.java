package com.tiggle.autotrading.model;

import com.tiggle.autotrading.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_kiwoom_credential")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KiwoomCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "appkey", nullable = false, length = 500)
    private String appkey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secretkey", nullable = false, length = 500)
    private String secretkey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public KiwoomCredential(Long userId, String appkey, String secretkey) {
        this.userId = userId;
        this.appkey = appkey;
        this.secretkey = secretkey;
    }
}
