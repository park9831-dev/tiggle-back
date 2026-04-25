package com.tiggle.autotrading.service;

import com.tiggle.autotrading.config.KiwoomApiConfig;
import com.tiggle.autotrading.dto.StockBasicInfoResponse;
import com.tiggle.autotrading.dto.StockListResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KiwoomStockService {

    private static final Logger log = LoggerFactory.getLogger(KiwoomStockService.class);

    // TODO: 키움 포털(openapi.kiwoom.com) 종목정보 카테고리에서 전체 종목 리스트 TR코드 확인 후 교체
    private static final String TR_STOCK_LIST = "ka10099";

    private final RestTemplate kiwoomRestTemplate;
    private final KiwoomApiConfig kiwoomApiConfig;
    private final KiwoomTokenService kiwoomTokenService;

    public KiwoomStockService(RestTemplate kiwoomRestTemplate,
                              KiwoomApiConfig kiwoomApiConfig,
                              KiwoomTokenService kiwoomTokenService) {
        this.kiwoomRestTemplate = kiwoomRestTemplate;
        this.kiwoomApiConfig = kiwoomApiConfig;
        this.kiwoomTokenService = kiwoomTokenService;
    }

    /**
     * 단일 종목 기본정보 조회 (ka10001)
     * @param stkCd 종목코드 (예: "005930")
     */
    @SuppressWarnings("unchecked")
    public StockBasicInfoResponse getStockBasicInfo(Long userId, String stkCd) {
        log.info("[KiwoomStock] userId={} 종목기본정보 조회 stkCd={}", userId, stkCd);

        Map<String, Object> raw = callStkinfo(userId, "ka10001", Map.of("stk_cd", stkCd), null, null);

        return new StockBasicInfoResponse(
                stkCd,
                (String) raw.get("stk_nm"),
                (String) raw.get("mrkt_tp"),
                (String) raw.get("upjong_cd"),
                (String) raw.get("upjong_nm"),
                (String) raw.get("face_val"),
                (String) raw.get("lst_stk_qty"),
                (String) raw.get("capital"),
                (String) raw.get("ipo_dt"),
                (String) raw.get("settle_month"),
                toInt(raw.get("return_code")),
                (String) raw.get("return_msg")
        );
    }

    /**
     * 시장별 전체 종목 리스트 조회 (페이지네이션 지원)
     * @param mrktTp 시장구분 (1=코스피, 2=코스닥)
     * @param nextKey 다음 페이지 키 (첫 조회 시 null)
     */
    @SuppressWarnings("unchecked")
    public StockListResponse getStockList(Long userId, String mrktTp, String nextKey) {
        log.info("[KiwoomStock] userId={} 종목리스트 조회 mrktTp={} nextKey={}", userId, mrktTp, nextKey);

        Map<String, String> body = Map.of("mrkt_tp", mrktTp);
        Map<String, Object> raw = callStkinfo(userId, TR_STOCK_LIST, body, "Y", nextKey);

        List<Map<String, Object>> items = (List<Map<String, Object>>) raw.getOrDefault("stk_list", List.of());
        List<StockListResponse.StockItem> stocks = items.stream()
                .map(item -> new StockListResponse.StockItem(
                        (String) item.get("stk_cd"),
                        (String) item.get("stk_nm"),
                        (String) item.get("mrkt_tp"),
                        (String) item.get("upjong_cd")
                ))
                .toList();

        return new StockListResponse(
                stocks,
                (String) raw.get("cont_yn"),
                (String) raw.get("next_key"),
                toInt(raw.get("return_code")),
                (String) raw.get("return_msg")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callStkinfo(Long userId, String apiId,
                                            Map<String, String> body,
                                            String contYn, String nextKey) {
        String token = kiwoomTokenService.getToken(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + token);
        headers.set("api-id", apiId);
        if (contYn != null) headers.set("cont-yn", contYn);
        if (nextKey != null && !nextKey.isBlank()) headers.set("next-key", nextKey);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = kiwoomRestTemplate.postForEntity(
                    kiwoomApiConfig.getStkinfoUrl(), entity, Map.class);

            Map<String, Object> raw = response.getBody();
            if (raw == null) throw new IllegalStateException("키움 API 응답이 비어 있습니다. apiId=" + apiId);

            return raw;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KiwoomStock] userId={} apiId={} 오류 status={}", userId, apiId, e.getStatusCode());
            throw new IllegalStateException("키움 API 호출 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value != null) return Integer.parseInt(value.toString());
        return null;
    }
}
