package com.tiggle.autotrading.dto;

import jakarta.validation.constraints.NotBlank;

public record KiwoomCredentialRequest(
        @NotBlank(message = "appkey는 필수입니다.")
        String appkey,

        @NotBlank(message = "secretkey는 필수입니다.")
        String secretkey
) {}
