package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.ledger.model.SettlementTransaction;
import com.swiftpay.ledger.repository.SettlementTransactionRepository;
import com.swiftpay.ledger.repository.TransactionJpaRepository;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SettlementTransactionPostgresRepository implements SettlementTransactionRepository {

    private final TransactionJpaRepository jpa;

    public SettlementTransactionPostgresRepository(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SettlementTransaction> findByTransactionIdForUpdate(String transactionId) {
        return jpa.findByTransactionIdForUpdate(transactionId).map(this::toModel);
    }

    @Override
    public void updateStatus(String transactionId, TransactionStatus status) {
        int updated = jpa.updateStatusIfPending(transactionId, status, TransactionStatus.PENDING);
        if (updated == 0) {
            throw new IllegalStateException(
                    "Cannot update transaction " + transactionId + " — not found or not PENDING");
        }
    }

    private SettlementTransaction toModel(PaymentTransaction entity) {
        return new SettlementTransaction(entity.getTransactionId(), entity.getStatus());
    }
}
