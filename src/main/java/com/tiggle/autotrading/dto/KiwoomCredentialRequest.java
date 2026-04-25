package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "키움 API 자격증명 등록·수정 요청")
public record KiwoomCredentialRequest(
        @Schema(description = "키움 OpenAPI 앱키", example = "PSabc1234...")
        @NotBlank(message = "appkey는 필수입니다.")
        String appkey,

        @Schema(description = "키움 OpenAPI 시크릿키", example = "abcXYZ5678...")
        @NotBlank(message = "secretkey는 필수입니다.")
        String secretkey
) {}
