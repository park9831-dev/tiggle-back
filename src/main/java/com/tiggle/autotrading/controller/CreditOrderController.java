package com.tiggle.autotrading.controller;

import com.tiggle.autotrading.dto.CreditBuyOrderRequest;
import com.tiggle.autotrading.dto.CreditCancelOrderRequest;
import com.tiggle.autotrading.dto.CreditModifyOrderRequest;
import com.tiggle.autotrading.dto.CreditOrderResponse;
import com.tiggle.autotrading.dto.CreditSellOrderRequest;
import com.tiggle.autotrading.service.KiwoomCreditOrderService;
import com.tiggle.autotrading.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신용주문", description = "키움증권 신용 매수·매도·정정·취소 주문")
@RestController
@RequestMapping("/api/v1/credit-orders")
public class CreditOrderController {

    private static final Logger log = LoggerFactory.getLogger(CreditOrderController.class);

    private final KiwoomCreditOrderService kiwoomCreditOrderService;
    private final UserService userService;

    public CreditOrderController(KiwoomCreditOrderService kiwoomCreditOrderService,
                                 UserService userService) {
        this.kiwoomCreditOrderService = kiwoomCreditOrderService;
        this.userService = userService;
    }

    @Operation(summary = "신용 매수주문", description = "키움증권 신용 매수주문을 요청합니다. (TR: kt10006)")
    @PostMapping("/buy")
    public ResponseEntity<CreditOrderResponse> buy(@Valid @RequestBody CreditBuyOrderRequest request,
                                                   Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("신용매수주문 요청 userId={} stkCd={}", userId, request.stkCd());
        return ResponseEntity.ok(kiwoomCreditOrderService.placeBuyOrder(userId, request));
    }

    @Operation(summary = "신용 매도주문", description = "키움증권 신용 매도주문을 요청합니다. (TR: kt10007) crdDealTp=33(융자)인 경우 crdLoanDt(대출일) 필수.")
    @PostMapping("/sell")
    public ResponseEntity<CreditOrderResponse> sell(@Valid @RequestBody CreditSellOrderRequest request,
                                                    Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("신용매도주문 요청 userId={} stkCd={}", userId, request.stkCd());
        return ResponseEntity.ok(kiwoomCreditOrderService.placeSellOrder(userId, request));
    }

    @Operation(summary = "신용 정정주문", description = "키움증권 신용 정정주문을 요청합니다. (TR: kt10008)")
    @PostMapping("/modify")
    public ResponseEntity<CreditOrderResponse> modify(@Valid @RequestBody CreditModifyOrderRequest request,
                                                      Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("신용정정주문 요청 userId={} origOrdNo={}", userId, request.origOrdNo());
        return ResponseEntity.ok(kiwoomCreditOrderService.modifyOrder(userId, request));
    }

    @Operation(summary = "신용 취소주문", description = "키움증권 신용 취소주문을 요청합니다. (TR: kt10009) cnclQty=0 입력 시 잔량 전체 취소.")
    @PostMapping("/cancel")
    public ResponseEntity<CreditOrderResponse> cancel(@Valid @RequestBody CreditCancelOrderRequest request,
                                                      Authentication authentication) {
        Long userId = resolveUserId(authentication);
        log.info("신용취소주문 요청 userId={} origOrdNo={}", userId, request.origOrdNo());
        return ResponseEntity.ok(kiwoomCreditOrderService.cancelOrder(userId, request));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."))
                .getId();
    }
}
