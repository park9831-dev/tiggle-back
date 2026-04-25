package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.KiwoomCredentialRequest;
import com.tiggle.autotrading.service.KiwoomCredentialService;
import com.tiggle.autotrading.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/kiwoom/credential")
public class KiwoomCredentialController {

    private final KiwoomCredentialService kiwoomCredentialService;
    private final UserService userService;

    public KiwoomCredentialController(KiwoomCredentialService kiwoomCredentialService,
                                      UserService userService) {
        this.kiwoomCredentialService = kiwoomCredentialService;
        this.userService = userService;
    }

    @PutMapping
    public ResponseEntity<?> saveCredential(@Valid @RequestBody KiwoomCredentialRequest request,
                                            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        kiwoomCredentialService.saveOrUpdate(userId, request.appkey(), request.secretkey());
        return ResponseEntity.ok(Map.of("message", "키움 API 인증정보가 저장되었습니다."));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCredential(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        kiwoomCredentialService.delete(userId);
        return ResponseEntity.ok(Map.of("message", "키움 API 인증정보가 삭제되었습니다."));
    }

    @GetMapping("/status")
    public ResponseEntity<?> credentialStatus(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        boolean registered = kiwoomCredentialService.hasCredential(userId);
        return ResponseEntity.ok(Map.of("registered", registered));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."))
                .getId();
    }
}
