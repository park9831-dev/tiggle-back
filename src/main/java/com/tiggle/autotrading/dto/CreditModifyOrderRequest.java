package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "신용 정정주문 요청 (TR: kt10008)")
public record CreditModifyOrderRequest(
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

        @Schema(description = "정정수량", example = "5")
        @NotBlank(message = "정정수량은 필수입니다.")
        String mdfyQty,

        @Schema(description = "정정단가", example = "69000")
        @NotBlank(message = "정정단가는 필수입니다.")
        String mdfyUv,

        @Schema(description = "정정조건단가 (조건부지정가 정정 시 사용)")
        String mdfyCondUv
) {}
