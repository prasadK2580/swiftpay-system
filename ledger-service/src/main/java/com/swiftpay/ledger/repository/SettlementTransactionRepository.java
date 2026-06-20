package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.model.SettlementTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.util.Optional;

/** Transaction row access during settlement (PostgreSQL). */
public interface SettlementTransactionRepository {

    Optional<SettlementTransaction> findByTransactionIdForUpdate(String transactionId);

    void updateStatus(String transactionId, TransactionStatus status);
}
