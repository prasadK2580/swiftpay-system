package com.swiftpay.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LedgerHttpProperties.class)
public class LedgerHttpClientConfig {

    @Bean
    public RestClient ledgerRestClient(LedgerHttpProperties properties, RestTemplateBuilder builder) {
        return RestClient.builder()
                .requestFactory(builder
                        .connectTimeout(properties.getConnectTimeout())
                        .readTimeout(properties.getReadTimeout())
                        .buildRequestFactory())
                .build();
    }
}
