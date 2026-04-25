package com.tiggle.autotrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "종목 리스트 응답")
public record StockListResponse(
        @Schema(description = "종목 목록")
        List<StockItem> stocks,

        @Schema(description = "다음 페이지 존재 여부 (Y/N)")
        String contYn,

        @Schema(description = "다음 페이지 조회 키")
        String nextKey,

        @Schema(description = "응답 코드 (0=성공)")
        Integer returnCode,

        @Schema(description = "응답 메시지")
        String returnMsg
) {
    @Schema(description = "종목 항목")
    public record StockItem(
            @Schema(description = "종목코드", example = "005930")
            String stkCd,

            @Schema(description = "종목명", example = "삼성전자")
            String stkNm,

            @Schema(description = "시장구분", example = "1")
            String mrktTp,

            @Schema(description = "업종코드", example = "004")
            String upjongCd
    ) {}
}
