package com.tiggle.autotrading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreditCancelOrderRequest(
        @NotBlank(message = "국내거래소구분은 필수입니다.")
        @Pattern(regexp = "KRX|NXT|SOR", message = "국내거래소구분은 KRX, NXT, SOR 중 하나여야 합니다.")
        String dmstStexTp,

        @NotBlank(message = "원주문번호는 필수입니다.")
        String origOrdNo,

        @NotBlank(message = "종목코드는 필수입니다.")
        String stkCd,

        // "0" 입력 시 잔량 전부 취소
        @NotBlank(message = "취소수량은 필수입니다.")
        String cnclQty
) {}
