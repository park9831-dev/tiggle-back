package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.KiwoomTokenResponse;
import com.tiggle.autotrading.service.KiwoomTokenService;
import com.tiggle.autotrading.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "키움 토큰", description = "키움증권 OAuth2 접근토큰 발급·폐기")
@RestController
@RequestMapping("/api/v1/kiwoom/token")
public class KiwoomTokenController {

    private static final Logger log = LoggerFactory.getLogger(KiwoomTokenController.class);

    private final KiwoomTokenService kiwoomTokenService;
    private final UserService userService;

    public KiwoomTokenController(KiwoomTokenService kiwoomTokenService, UserService userService) {
        this.kiwoomTokenService = kiwoomTokenService;
        this.userService = userService;
    }

    @Operation(summary = "접근토큰 수동 발급", description = "키움증권 OAuth2 접근토큰을 수동으로 발급합니다. 이미 유효한 토큰이 있으면 재사용됩니다.")
    @PostMapping("/issue")
    public ResponseEntity<KiwoomTokenResponse> issue(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("키움 접근토큰 수동 발급 요청 userId={}", userId);
        return ResponseEntity.ok(kiwoomTokenService.issueToken(userId));
    }

    @Operation(summary = "접근토큰 폐기", description = "발급된 키움증권 접근토큰을 폐기하고 서버 메모리에서 제거합니다.")
    @DeleteMapping("/revoke")
    public ResponseEntity<KiwoomTokenResponse> revoke(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("키움 접근토큰 폐기 요청 userId={}", userId);
        return ResponseEntity.ok(kiwoomTokenService.revokeToken(userId));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."))
                .getId();
    }
}
