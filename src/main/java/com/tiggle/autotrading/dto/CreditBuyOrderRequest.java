package com.tiggle.autotrading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreditBuyOrderRequest(
        @NotBlank(message = "국내거래소구분은 필수입니다.")
        @Pattern(regexp = "KRX|NXT|SOR", message = "국내거래소구분은 KRX, NXT, SOR 중 하나여야 합니다.")
        String dmstStexTp,

        @NotBlank(message = "종목코드는 필수입니다.")
        String stkCd,

        @NotBlank(message = "주문수량은 필수입니다.")
        String ordQty,

        String ordUv,

        @NotBlank(message = "매매구분은 필수입니다.")
        String trdeTp,

        String condUv
) {}
