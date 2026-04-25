package com.tiggle.autotrading.dto;

public record CreditOrderResponse(
        String ordNo,
        String dmstStexTp,
        String baseOrigOrdNo,
        String mdfyQty,
        String cnclQty,
        Integer returnCode,
        String returnMsg
) {}
