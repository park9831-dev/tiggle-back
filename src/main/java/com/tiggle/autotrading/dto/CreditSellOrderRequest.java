package com.tiggle.autotrading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreditSellOrderRequest(
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

        @NotBlank(message = "신용거래구분은 필수입니다.")
        @Pattern(regexp = "33|99", message = "신용거래구분은 33(융자) 또는 99(융자합)여야 합니다.")
        String crdDealTp,

        // 융자(crdDealTp=33)인 경우 필수, YYYYMMDD
        String crdLoanDt,

        String condUv
) {}
