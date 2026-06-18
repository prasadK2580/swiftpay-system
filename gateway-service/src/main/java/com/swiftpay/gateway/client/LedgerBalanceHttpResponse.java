package com.swiftpay.gateway.client;

/**
 * JSON mapping for ledger GET /v1/accounts/{userId}/balance.
 */
public record LedgerBalanceHttpResponse(Long userId, Double balance, String currency) {
}
