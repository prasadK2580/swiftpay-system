package com.swiftpay.gateway.infrastructure.http;

import com.swiftpay.gateway.config.LedgerHttpProperties;
import com.swiftpay.gateway.port.LedgerBalanceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.gateway.ledger-balance.source", havingValue = "http", matchIfMissing = true)
public class HttpLedgerBalanceReader implements LedgerBalanceReader {

    private static final Logger log = LoggerFactory.getLogger(HttpLedgerBalanceReader.class);

    private final RestClient ledgerRestClient;
    private final LedgerHttpProperties properties;

    public HttpLedgerBalanceReader(RestClient ledgerRestClient, LedgerHttpProperties properties) {
        this.ledgerRestClient = ledgerRestClient;
        this.properties = properties;
    }


    @Override
    public Optional<Double> getBalance(Long userId, String currency) {
        String url = balanceUrl(userId, currency);
        try {
            LedgerBalanceHttpResponse body = ledgerRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(LedgerBalanceHttpResponse.class);
            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(body.balance());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Ledger balance request failed userId={} url={}", userId, url, ex);
            return Optional.empty();
        }
    }

    private String balanceUrl(Long userId, String currency) {
        return UriComponentsBuilder.fromHttpUrl(resolveBaseUrl())
                .path("/v1/accounts/{userId}/balance")
                .queryParam("currency", currency)
                .buildAndExpand(userId)
                .toUriString();
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(properties.getBaseUrl())) {
            return trimTrailingSlash(properties.getBaseUrl());
        }
        return "http://localhost:8081";
    }

    private static String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
