package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "신용 매수주문 요청 (TR: kt10006)")
public record CreditBuyOrderRequest(
        @Schema(description = "국내거래소구분", allowableValues = {"KRX", "NXT", "SOR"}, example = "KRX")
        @NotBlank(message = "국내거래소구분은 필수입니다.")
        @Pattern(regexp = "KRX|NXT|SOR", message = "국내거래소구분은 KRX, NXT, SOR 중 하나여야 합니다.")
        String dmstStexTp,

        @Schema(description = "종목코드", example = "005930")
        @NotBlank(message = "종목코드는 필수입니다.")
        String stkCd,

        @Schema(description = "주문수량", example = "10")
        @NotBlank(message = "주문수량은 필수입니다.")
        String ordQty,

        @Schema(description = "주문단가 (시장가 주문 시 생략 가능)", example = "70000")
        String ordUv,

        @Schema(description = "매매구분 (0=보통, 3=시장가, 5=조건부지정가 등)", example = "0")
        @NotBlank(message = "매매구분은 필수입니다.")
        String trdeTp,

        @Schema(description = "조건단가 (조건부지정가 주문 시 사용)")
        String condUv
) {}
