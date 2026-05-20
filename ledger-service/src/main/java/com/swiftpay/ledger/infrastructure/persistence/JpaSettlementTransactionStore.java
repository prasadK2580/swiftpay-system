package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.ledger.model.SettlementTransaction;
import com.swiftpay.ledger.port.SettlementTransactionStore;
import com.swiftpay.ledger.repo.TransactionRepository;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaSettlementTransactionStore implements SettlementTransactionStore {

    private final TransactionRepository transactionRepository;

    public JpaSettlementTransactionStore(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    @Override
    public Optional<SettlementTransaction> findByTransactionIdForUpdate(String transactionId) {
        return transactionRepository.findByTransactionIdForUpdate(transactionId).map(this::toModel);
    }

    @Override
    public void updateStatus(String transactionId, TransactionStatus status) {
        PaymentTransaction entity = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));
        entity.setStatus(status);
        transactionRepository.save(entity);
    }

    private SettlementTransaction toModel(PaymentTransaction entity) {
        return new SettlementTransaction(entity.getTransactionId(), entity.getStatus());
    }
}
