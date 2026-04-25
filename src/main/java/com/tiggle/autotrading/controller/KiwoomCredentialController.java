package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.KiwoomCredentialRequest;
import com.tiggle.autotrading.service.KiwoomCredentialService;
import com.tiggle.autotrading.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "키움 인증정보", description = "키움증권 API 자격증명(appkey/secretkey) AES-256-GCM 암호화 저장 관리")
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

    @Operation(summary = "인증정보 등록·수정", description = "appkey/secretkey를 AES-256-GCM으로 암호화하여 DB에 저장합니다. 이미 등록된 경우 덮어씁니다.")
    @PutMapping
    public ResponseEntity<?> saveCredential(@Valid @RequestBody KiwoomCredentialRequest request,
                                            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        kiwoomCredentialService.saveOrUpdate(userId, request.appkey(), request.secretkey());
        return ResponseEntity.ok(Map.of("message", "키움 API 인증정보가 저장되었습니다."));
    }

    @Operation(summary = "인증정보 삭제", description = "등록된 appkey/secretkey를 DB에서 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<?> deleteCredential(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        kiwoomCredentialService.delete(userId);
        return ResponseEntity.ok(Map.of("message", "키움 API 인증정보가 삭제되었습니다."));
    }

    @Operation(summary = "인증정보 등록 여부 확인", description = "현재 사용자의 appkey/secretkey 등록 여부를 반환합니다.")
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
