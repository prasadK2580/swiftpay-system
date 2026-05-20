package com.swiftpay.ledger.model;

import com.swiftpay.shared.domain.enums.TransactionStatus;

/**
 * Ledger view of a payment transaction row during settlement.
 */
public record SettlementTransaction(String transactionId, TransactionStatus status) {
}
