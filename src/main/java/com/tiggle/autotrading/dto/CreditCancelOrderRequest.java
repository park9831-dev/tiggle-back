package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "신용 취소주문 요청 (TR: kt10009)")
public record CreditCancelOrderRequest(
        @Schema(description = "국내거래소구분", allowableValues = {"KRX", "NXT", "SOR"}, example = "KRX")
        @NotBlank(message = "국내거래소구분은 필수입니다.")
        @Pattern(regexp = "KRX|NXT|SOR", message = "국내거래소구분은 KRX, NXT, SOR 중 하나여야 합니다.")
        String dmstStexTp,

        @Schema(description = "원주문번호", example = "0000012345")
        @NotBlank(message = "원주문번호는 필수입니다.")
        String origOrdNo,

        @Schema(description = "종목코드", example = "005930")
        @NotBlank(message = "종목코드는 필수입니다.")
        String stkCd,

        @Schema(description = "취소수량 (0 입력 시 잔량 전체 취소)", example = "0")
        @NotBlank(message = "취소수량은 필수입니다.")
        String cnclQty
) {}
