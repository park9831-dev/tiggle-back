package com.tiggle.autotrading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KiwoomApiConfig {

    @Value("${kiwoom.api.base-url}")
    private String baseUrl;

    @Value("${kiwoom.api.mock-base-url}")
    private String mockBaseUrl;

    @Value("${kiwoom.api.order-path}")
    private String orderPath;

    @Value("${kiwoom.api.stkinfo-path}")
    private String stkinfoPath;

    @Value("${kiwoom.api.token-path}")
    private String tokenPath;

    @Value("${kiwoom.api.revoke-path}")
    private String revokePath;

    @Value("${kiwoom.api.appkey:}")
    private String appkey;

    @Value("${kiwoom.api.secretkey:}")
    private String secretkey;

    @Bean
    public RestTemplate kiwoomRestTemplate() {
        return new RestTemplate();
    }

    public String getOrderUrl() {
        return baseUrl + orderPath;
    }

    public String getStkinfoUrl() {
        return baseUrl + stkinfoPath;
    }

    public String getTokenUrl() {
        return baseUrl + tokenPath;
    }

    public String getRevokeUrl() {
        return baseUrl + revokePath;
    }

    public String getAppkey() {
        return appkey;
    }

    public String getSecretkey() {
        return secretkey;
    }
}
