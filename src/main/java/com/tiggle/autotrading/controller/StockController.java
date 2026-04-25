package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.StockBasicInfoResponse;
import com.tiggle.autotrading.dto.StockListResponse;
import com.tiggle.autotrading.service.KiwoomStockService;
import com.tiggle.autotrading.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "종목정보", description = "키움증권 종목 기본정보 및 종목 리스트 조회")
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final KiwoomStockService kiwoomStockService;
    private final UserService userService;

    public StockController(KiwoomStockService kiwoomStockService, UserService userService) {
        this.kiwoomStockService = kiwoomStockService;
        this.userService = userService;
    }

    @Operation(
            summary = "종목 기본정보 조회",
            description = "종목코드로 종목명·시장구분·업종·상장일 등 기본정보를 조회합니다. (키움 TR: ka10001)"
    )
    @GetMapping("/{stkCd}")
    public ResponseEntity<StockBasicInfoResponse> getStockBasicInfo(
            @PathVariable
            @Parameter(description = "종목코드", example = "005930")
            String stkCd,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(kiwoomStockService.getStockBasicInfo(userId, stkCd));
    }

    @Operation(
            summary = "시장별 종목 리스트 조회",
            description = "시장구분(1=코스피, 2=코스닥)으로 전체 종목 목록을 조회합니다. " +
                    "nextKey 파라미터로 페이지네이션을 지원합니다. " +
                    "※ TR코드(ka10099)는 키움 포털 확인 후 교체 필요"
    )
    @GetMapping
    public ResponseEntity<StockListResponse> getStockList(
            @RequestParam
            @Parameter(description = "시장구분 (1=코스피, 2=코스닥)", example = "1")
            String mrktTp,
            @RequestParam(required = false)
            @Parameter(description = "페이지네이션 키 (응답의 nextKey 값 사용, 첫 조회 시 생략)")
            String nextKey,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(kiwoomStockService.getStockList(userId, mrktTp, nextKey));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."))
                .getId();
    }
}
