package com.swiftpay.ledger.port;

import com.swiftpay.ledger.model.SettlementTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.util.Optional;

/**
 * Narrow persistence port for settlement status updates (ISP, DIP).
 */
public interface SettlementTransactionStore {

    Optional<SettlementTransaction> findByTransactionIdForUpdate(String transactionId);

    void updateStatus(String transactionId, TransactionStatus status);
}
