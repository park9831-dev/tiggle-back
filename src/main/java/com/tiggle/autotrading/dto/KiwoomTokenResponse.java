package com.tiggle.autotrading.dto;

public record KiwoomTokenResponse(
        String token,
        String tokenType,
        String expiresDt,
        Integer returnCode,
        String returnMsg
) {}
