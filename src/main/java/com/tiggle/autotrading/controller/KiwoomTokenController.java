package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.KiwoomTokenResponse;
import com.tiggle.autotrading.service.KiwoomTokenService;
import com.tiggle.autotrading.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/issue")
    public ResponseEntity<KiwoomTokenResponse> issue(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("키움 접근토큰 수동 발급 요청 userId={}", userId);
        return ResponseEntity.ok(kiwoomTokenService.issueToken(userId));
    }

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
