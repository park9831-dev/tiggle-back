package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주식 기본정보 응답 (ka10001)")
public record StockBasicInfoResponse(
        @Schema(description = "종목코드", example = "005930")
        String stkCd,

        @Schema(description = "종목명", example = "삼성전자")
        String stkNm,

        @Schema(description = "시장구분 (1=코스피, 2=코스닥, 3=코넥스)", example = "1")
        String mrktTp,

        @Schema(description = "업종코드", example = "004")
        String upjongCd,

        @Schema(description = "업종명", example = "전기전자")
        String upjongNm,

        @Schema(description = "액면가", example = "100")
        String faceVal,

        @Schema(description = "상장주식수")
        String lstStkQty,

        @Schema(description = "자본금")
        String capital,

        @Schema(description = "상장일 YYYYMMDD", example = "19750611")
        String ipoDt,

        @Schema(description = "결산월", example = "12")
        String settleMonth,

        @Schema(description = "응답 코드 (0=성공)")
        Integer returnCode,

        @Schema(description = "응답 메시지")
        String returnMsg
) {}
