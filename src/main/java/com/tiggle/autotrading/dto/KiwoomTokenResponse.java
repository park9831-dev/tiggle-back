package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "키움 접근토큰 발급·폐기 응답")
public record KiwoomTokenResponse(
        @Schema(description = "발급된 접근토큰 값")
        String token,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "토큰 만료일시", example = "2024-01-01 23:59:59")
        String expiresDt,

        @Schema(description = "응답 코드 (0=성공)")
        Integer returnCode,

        @Schema(description = "응답 메시지")
        String returnMsg
) {}
