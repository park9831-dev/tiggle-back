package com.tiggle.autotrading.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.tiggle.autotrading.dto.AuthResponse;
import com.tiggle.autotrading.dto.LoginRequest;
import com.tiggle.autotrading.dto.PasswordChangeRequest;
import com.tiggle.autotrading.dto.SignupRequest;
import com.tiggle.autotrading.dto.TokenResponse;
import com.tiggle.autotrading.dto.UserResponse;
import com.tiggle.autotrading.dto.UserUpdateRequest;
import com.tiggle.autotrading.model.RefreshToken;
import com.tiggle.autotrading.model.User;
import com.tiggle.autotrading.security.JwtService;
import com.tiggle.autotrading.service.RefreshTokenService;
import com.tiggle.autotrading.service.UserLogService;
import com.tiggle.autotrading.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private static final String LOG_TYPE_LOGIN = "LOGIN";
    private static final String LOG_SUCCESS = "SUCCESS";
    private static final String LOG_FAILED = "FAILED";

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserLogService userLogService;
    private final long accessTokenExpiresIn;

    public UserController(UserService userService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UserLogService userLogService,
                          @Value("${jwt.expiration-ms:3600000}") long accessTokenExpirationMs) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userLogService = userLogService;
        this.accessTokenExpiresIn = accessTokenExpirationMs / 1000;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String accessDevice = getAccessDevice(httpRequest);

        var userOpt = userService.authenticate(request.emailId(), request.password());
        if (userOpt.isEmpty()) {
            userService.findByEmail(request.emailId())
                    .ifPresent(user -> userLogService.saveLog(
                            user.getId(), clientIp, accessDevice, LOG_TYPE_LOGIN, LOG_FAILED));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("인증 실패: 이메일 또는 비밀번호를 확인하세요."));
        }
        User user = userOpt.get();

        userLogService.saveLog(user.getId(), clientIp, accessDevice, LOG_TYPE_LOGIN, LOG_SUCCESS);

        String accessToken = jwtService.generateToken(user.getEmail(), user.getAuthority());
        RefreshToken refreshToken = refreshTokenService.createOrUpdateToken(user.getId(), clientIp);

        return ResponseEntity.ok(new TokenResponse(
                accessToken,
                refreshToken.getToken(),
                accessTokenExpiresIn,
                "로그인 성공"
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request, Authentication authentication) {
        if (authentication == null ||
            !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("관리자 권한이 필요합니다."));
        }

        try {
            boolean created = userService.register(
                    request.email(),
                    request.userName(),
                    request.mobile(),
                    request.userType(),
                    request.password()
            );
            if (!created) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new AuthResponse("이미 사용 중인 이메일입니다."));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse("회원가입 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("입력 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUserList(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("관리자 권한이 필요합니다."));
        }

        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody UserUpdateRequest request,
                                        Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            User updated = userService.updateUser(
                    id,
                    request.userName(),
                    request.mobile(),
                    request.userType(),
                    request.usedBool()
            );
            return ResponseEntity.ok(UserResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("수정 오류: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(new AuthResponse("사용자 삭제 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("삭제 오류: " + e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id,
                                            @RequestBody PasswordChangeRequest request,
                                            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            userService.changePassword(id, request.newPassword());
            return ResponseEntity.ok(new AuthResponse("비밀번호 변경 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("비밀번호 변경 오류: " + e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockAccount(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            User unlocked = userService.unlockAccount(id);
            return ResponseEntity.ok(UserResponse.from(unlocked));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("잠금 해제 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/users/check-email")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam String email, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            boolean exists = userService.isEmailExists(email);
            return ResponseEntity.ok(new EmailCheckResponse(
                    !exists,
                    exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("권한이 없습니다."));
        }

        try {
            String tempPassword = userService.resetPassword(id);
            return ResponseEntity.ok(new PasswordResetResponse(true, "임시 비밀번호가 발급되었습니다.", tempPassword));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("비밀번호 초기화 오류: " + e.getMessage()));
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null &&
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String getAccessDevice(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record EmailCheckResponse(boolean available, String message) {}
    private record PasswordResetResponse(boolean success, String message, String tempPassword) {}
}
