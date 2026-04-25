package com.tiggle.autotrading.service;

import com.tiggle.autotrading.config.KiwoomApiConfig;
import com.tiggle.autotrading.dto.KiwoomTokenResponse;
import com.tiggle.autotrading.model.KiwoomCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KiwoomTokenService {

    private static final Logger log = LoggerFactory.getLogger(KiwoomTokenService.class);
    private static final int REFRESH_THRESHOLD_MINUTES = 60;
    private static final DateTimeFormatter FMT_COMPACT  = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate kiwoomRestTemplate;
    private final KiwoomApiConfig kiwoomApiConfig;
    private final KiwoomCredentialService kiwoomCredentialService;

    // userId → 발급된 토큰 정보
    private final Map<Long, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    public KiwoomTokenService(RestTemplate kiwoomRestTemplate,
                              KiwoomApiConfig kiwoomApiConfig,
                              KiwoomCredentialService kiwoomCredentialService) {
        this.kiwoomRestTemplate = kiwoomRestTemplate;
        this.kiwoomApiConfig = kiwoomApiConfig;
        this.kiwoomCredentialService = kiwoomCredentialService;
    }

    // 30분마다 만료 임박 토큰 자동 갱신
    @Scheduled(fixedDelay = 1_800_000)
    public void refreshExpiringTokens() {
        tokenCache.forEach((userId, entry) -> {
            if (entry.isExpiringSoon()) {
                log.info("[KiwoomToken] userId={} 토큰 만료 임박 — 자동 갱신", userId);
                try {
                    issueToken(userId);
                } catch (Exception e) {
                    log.error("[KiwoomToken] userId={} 자동 갱신 실패: {}", userId, e.getMessage());
                }
            }
        });
    }

    public KiwoomTokenResponse issueToken(Long userId) {
        KiwoomCredential credential = kiwoomCredentialService.getCredential(userId);
        return issueWithCredential(userId, credential.getAppkey(), credential.getSecretkey());
    }

    public KiwoomTokenResponse revokeToken(Long userId) {
        TokenEntry entry = tokenCache.get(userId);
        if (entry == null || entry.token().isBlank()) {
            throw new IllegalStateException("폐기할 유효한 토큰이 없습니다. userId=" + userId);
        }

        KiwoomCredential credential = kiwoomCredentialService.getCredential(userId);

        Map<String, String> body = Map.of(
                "appkey", credential.getAppkey(),
                "secretkey", credential.getSecretkey(),
                "token", entry.token()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = kiwoomRestTemplate.postForObject(
                    kiwoomApiConfig.getRevokeUrl(), new HttpEntity<>(body, headers), Map.class);

            if (raw == null) throw new IllegalStateException("토큰 폐기 응답이 비어 있습니다.");

            tokenCache.remove(userId);

            Integer returnCode = toInt(raw.get("return_code"));
            String returnMsg   = (String) raw.get("return_msg");
            log.info("[KiwoomToken] userId={} 토큰 폐기 완료 returnCode={}", userId, returnCode);
            return new KiwoomTokenResponse(null, null, null, returnCode, returnMsg);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KiwoomToken] userId={} 폐기 실패 status={}", userId, e.getStatusCode());
            throw new IllegalStateException("토큰 폐기 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * 유효한 토큰을 반환합니다. 캐시에 없거나 만료된 경우 자동 재발급합니다.
     */
    public String getToken(Long userId) {
        TokenEntry entry = tokenCache.get(userId);
        if (entry == null || !entry.isValid()) {
            log.info("[KiwoomToken] userId={} 캐시 미스 또는 만료 — 재발급", userId);
            issueToken(userId);
        }
        return tokenCache.get(userId).token();
    }

    public boolean isTokenValid(Long userId) {
        TokenEntry entry = tokenCache.get(userId);
        return entry != null && entry.isValid();
    }

    // ── 내부 ────────────────────────────────────────────────────────────────

    private KiwoomTokenResponse issueWithCredential(Long userId, String appkey, String secretkey) {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appkey,
                "secretkey", secretkey
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = kiwoomRestTemplate.postForObject(
                    kiwoomApiConfig.getTokenUrl(), new HttpEntity<>(body, headers), Map.class);

            if (raw == null) throw new IllegalStateException("토큰 발급 응답이 비어 있습니다.");

            String token      = (String) raw.get("token");
            String expiresDt  = (String) raw.get("expires_dt");
            String tokenType  = (String) raw.get("token_type");
            Integer returnCode = toInt(raw.get("return_code"));
            String returnMsg  = (String) raw.get("return_msg");

            tokenCache.put(userId, new TokenEntry(token != null ? token : "", parseExpiry(expiresDt)));
            log.info("[KiwoomToken] userId={} 토큰 발급 완료. 만료={}", userId, expiresDt);

            return new KiwoomTokenResponse(token, tokenType, expiresDt, returnCode, returnMsg);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KiwoomToken] userId={} 발급 실패 status={}", userId, e.getStatusCode());
            throw new IllegalStateException("토큰 발급 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    private LocalDateTime parseExpiry(String expiresDt) {
        if (expiresDt == null || expiresDt.isBlank()) {
            return LocalDateTime.now().plusHours(24);
        }
        try { return LocalDateTime.parse(expiresDt, FMT_COMPACT); }
        catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(expiresDt, FMT_DATETIME); }
        catch (DateTimeParseException ignored) {}
        log.warn("[KiwoomToken] 만료일시 파싱 실패: {}. 24시간 후로 설정.", expiresDt);
        return LocalDateTime.now().plusHours(24);
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value != null) return Integer.parseInt(value.toString());
        return null;
    }

    private record TokenEntry(String token, LocalDateTime expiry) {
        boolean isValid() {
            return token != null && !token.isBlank() && LocalDateTime.now().isBefore(expiry);
        }
        boolean isExpiringSoon() {
            return isValid() && expiry.minusMinutes(REFRESH_THRESHOLD_MINUTES).isBefore(LocalDateTime.now());
        }
    }
}
