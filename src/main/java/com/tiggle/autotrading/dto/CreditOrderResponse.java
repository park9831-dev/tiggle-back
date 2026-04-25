package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신용주문 응답")
public record CreditOrderResponse(
        @Schema(description = "주문번호")
        String ordNo,

        @Schema(description = "국내거래소구분", example = "KRX")
        String dmstStexTp,

        @Schema(description = "원주문번호 (정정·취소 시)")
        String baseOrigOrdNo,

        @Schema(description = "정정수량")
        String mdfyQty,

        @Schema(description = "취소수량")
        String cnclQty,

        @Schema(description = "응답 코드 (0=성공)")
        Integer returnCode,

        @Schema(description = "응답 메시지")
        String returnMsg
) {}
