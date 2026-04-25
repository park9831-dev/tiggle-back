package com.tiggle.autotrading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Async MVC configuration used for long-lived responses such as SSE.
 */
@Configuration
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class AsyncConfig implements WebMvcConfigurer {

    private static final long ASYNC_TIMEOUT = 60 * 60 * 1000L; // 1 hour

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(ASYNC_TIMEOUT);
    }
}


