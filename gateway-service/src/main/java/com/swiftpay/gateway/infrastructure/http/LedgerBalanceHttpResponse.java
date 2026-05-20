package com.swiftpay.gateway.infrastructure.http;

/**
 * JSON mapping for ledger GET /v1/accounts/{userId}/balance (kept in gateway; shared does not depend on ledger).
 */
public record LedgerBalanceHttpResponse(Long userId, Double balance, String currency) {
}
