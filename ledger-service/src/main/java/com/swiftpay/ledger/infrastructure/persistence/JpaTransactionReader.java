package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.ledger.port.TransactionReader;
import com.swiftpay.ledger.repo.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaTransactionReader implements TransactionReader {

    private final TransactionRepository transactionRepository;

    public JpaTransactionReader(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    @Override
    public boolean existsByTransactionId(String transactionId) {
        return transactionId != null
                && transactionRepository.findByTransactionId(transactionId).isPresent();
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    @Override
    public Optional<PaymentTransaction> findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<PaymentTransaction> findHistoryForUser(Long userId, int limit) {
        return transactionRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
                userId, userId, PageRequest.of(0, limit));
    }
}
