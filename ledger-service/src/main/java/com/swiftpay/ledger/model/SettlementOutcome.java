package com.swiftpay.ledger.model;
/**
 * Result of ledger settlement — drives Kafka status emission (no direct PG_TX write).
 */
public enum SettlementOutcome {
    COMPLETED,
    FAILED,
    SKIPPED
}
