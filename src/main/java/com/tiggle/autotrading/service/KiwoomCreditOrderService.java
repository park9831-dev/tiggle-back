package com.tiggle.autotrading.service;

import com.tiggle.autotrading.config.KiwoomApiConfig;
import com.tiggle.autotrading.dto.CreditBuyOrderRequest;
import com.tiggle.autotrading.dto.CreditCancelOrderRequest;
import com.tiggle.autotrading.dto.CreditModifyOrderRequest;
import com.tiggle.autotrading.dto.CreditOrderResponse;
import com.tiggle.autotrading.dto.CreditSellOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KiwoomCreditOrderService {

    private static final Logger log = LoggerFactory.getLogger(KiwoomCreditOrderService.class);

    private final RestTemplate kiwoomRestTemplate;
    private final KiwoomApiConfig kiwoomApiConfig;
    private final KiwoomTokenService kiwoomTokenService;

    public KiwoomCreditOrderService(RestTemplate kiwoomRestTemplate,
                                    KiwoomApiConfig kiwoomApiConfig,
                                    KiwoomTokenService kiwoomTokenService) {
        this.kiwoomRestTemplate = kiwoomRestTemplate;
        this.kiwoomApiConfig = kiwoomApiConfig;
        this.kiwoomTokenService = kiwoomTokenService;
    }

    public CreditOrderResponse placeBuyOrder(Long userId, CreditBuyOrderRequest req) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("dmst_stex_tp", req.dmstStexTp());
        body.put("stk_cd", req.stkCd());
        body.put("ord_qty", req.ordQty());
        if (req.ordUv() != null && !req.ordUv().isBlank()) body.put("ord_uv", req.ordUv());
        body.put("trde_tp", req.trdeTp());
        if (req.condUv() != null && !req.condUv().isBlank()) body.put("cond_uv", req.condUv());

        log.info("[KiwoomCreditOrder] userId={} 신용매수 stkCd={} qty={}", userId, req.stkCd(), req.ordQty());
        return callKiwoom(userId, "kt10006", body);
    }

    public CreditOrderResponse placeSellOrder(Long userId, CreditSellOrderRequest req) {
        if ("33".equals(req.crdDealTp()) && (req.crdLoanDt() == null || req.crdLoanDt().isBlank())) {
            throw new IllegalArgumentException("융자(crdDealTp=33) 매도 시 대출일(crdLoanDt)은 필수입니다.");
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("dmst_stex_tp", req.dmstStexTp());
        body.put("stk_cd", req.stkCd());
        body.put("ord_qty", req.ordQty());
        if (req.ordUv() != null && !req.ordUv().isBlank()) body.put("ord_uv", req.ordUv());
        body.put("trde_tp", req.trdeTp());
        body.put("crd_deal_tp", req.crdDealTp());
        if (req.crdLoanDt() != null && !req.crdLoanDt().isBlank()) body.put("crd_loan_dt", req.crdLoanDt());
        if (req.condUv() != null && !req.condUv().isBlank()) body.put("cond_uv", req.condUv());

        log.info("[KiwoomCreditOrder] userId={} 신용매도 stkCd={} crdDealTp={}", userId, req.stkCd(), req.crdDealTp());
        return callKiwoom(userId, "kt10007", body);
    }

    public CreditOrderResponse modifyOrder(Long userId, CreditModifyOrderRequest req) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("dmst_stex_tp", req.dmstStexTp());
        body.put("orig_ord_no", req.origOrdNo());
        body.put("stk_cd", req.stkCd());
        body.put("mdfy_qty", req.mdfyQty());
        body.put("mdfy_uv", req.mdfyUv());
        if (req.mdfyCondUv() != null && !req.mdfyCondUv().isBlank()) body.put("mdfy_cond_uv", req.mdfyCondUv());

        log.info("[KiwoomCreditOrder] userId={} 신용정정 origOrdNo={}", userId, req.origOrdNo());
        return callKiwoom(userId, "kt10008", body);
    }

    public CreditOrderResponse cancelOrder(Long userId, CreditCancelOrderRequest req) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("dmst_stex_tp", req.dmstStexTp());
        body.put("orig_ord_no", req.origOrdNo());
        body.put("stk_cd", req.stkCd());
        body.put("cncl_qty", req.cnclQty());

        log.info("[KiwoomCreditOrder] userId={} 신용취소 origOrdNo={}", userId, req.origOrdNo());
        return callKiwoom(userId, "kt10009", body);
    }

    @SuppressWarnings("unchecked")
    private CreditOrderResponse callKiwoom(Long userId, String apiId, Map<String, String> body) {
        String token = kiwoomTokenService.getToken(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + token);
        headers.set("api-id", apiId);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = kiwoomRestTemplate.postForEntity(
                    kiwoomApiConfig.getOrderUrl(), entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IllegalStateException("키움 API 응답이 비어 있습니다. apiId=" + apiId);
            }

            return new CreditOrderResponse(
                    (String) responseBody.get("ord_no"),
                    (String) responseBody.get("dmst_stex_tp"),
                    (String) responseBody.get("base_orig_ord_no"),
                    (String) responseBody.get("mdfy_qty"),
                    (String) responseBody.get("cncl_qty"),
                    responseBody.get("return_code") instanceof Integer rc ? rc
                            : responseBody.get("return_code") != null
                            ? Integer.parseInt(responseBody.get("return_code").toString()) : null,
                    (String) responseBody.get("return_msg")
            );
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KiwoomCreditOrder] userId={} apiId={} 오류 status={}", userId, apiId, e.getStatusCode());
            throw new IllegalStateException("키움 API 호출 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }
}
