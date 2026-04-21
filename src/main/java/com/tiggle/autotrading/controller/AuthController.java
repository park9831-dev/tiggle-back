package com.tiggle.autotrading.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.tiggle.autotrading.dto.AuthResponse;
import com.tiggle.autotrading.dto.RefreshTokenRequest;
import com.tiggle.autotrading.dto.TokenResponse;
import com.tiggle.autotrading.model.RefreshToken;
import com.tiggle.autotrading.model.User;
import com.tiggle.autotrading.security.JwtService;
import com.tiggle.autotrading.service.RefreshTokenService;
import com.tiggle.autotrading.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final JwtService jwtService;
    private final long accessTokenExpiresIn;

    public AuthController(
            RefreshTokenService refreshTokenService,
            UserService userService,
            JwtService jwtService,
            @Value("${jwt.expiration-ms:3600000}") long accessTokenExpirationMs) {
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.accessTokenExpiresIn = accessTokenExpirationMs / 1000;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody RefreshTokenRequest request,
                                                HttpServletRequest httpRequest) {
        log.info("Refresh token request received");

        if (request.refreshToken() == null || request.refreshToken().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Refresh token is required."));
        }

        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.validateAndGetToken(request.refreshToken());
        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid or expired refresh token."));
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        Optional<User> userOpt = userService.getUserById(refreshToken.getUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("User not found."));
        }

        User user = userOpt.get();
        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getAuthority());
        String clientIp = getClientIp(httpRequest);
        RefreshToken rotatedToken = refreshTokenService.rotateToken(refreshToken, clientIp);

        log.info("Token refreshed for userId={}", user.getId());
        return ResponseEntity.ok(new TokenResponse(
                newAccessToken,
                rotatedToken.getToken(),
                accessTokenExpiresIn,
                "Token refreshed successfully."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            userService.findByEmail(email)
                    .ifPresent(user -> refreshTokenService.deleteByUserId(user.getId()));
        } else if (request != null && request.refreshToken() != null) {
            refreshTokenService.deleteByToken(request.refreshToken());
        }

        return ResponseEntity.ok(new AuthResponse("Logout completed."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
