package com.swiftpay.shared.domain;

/**
 * Canonical settlement failure reasons (Service B → Kafka → Service A).
 */
public final class SettlementFailureReason {

    public static final String INSUFFICIENT_FUNDS = "insufficient funds";
    public static final String CURRENCY_MISMATCH = "currency mismatch on ledger accounts";
    public static final String ACCOUNT_NOT_FOUND = "account not found";

    private SettlementFailureReason() {
    }
}
