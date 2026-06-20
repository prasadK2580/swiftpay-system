package com.swiftpay.gateway.repository;

import com.swiftpay.gateway.application.mapper.PaymentTransactionMapper;
import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Saves and loads payment rows in PostgreSQL (source of truth).
 */
@Repository
public class PaymentRepository {

    private final TransactionJpaRepository jpa;

    public PaymentRepository(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    public PaymentTransaction savePending(String transactionId, String idempotencyKey, PaymentCommand command) {
        PaymentTransaction row = PaymentTransactionMapper.toPendingEntity(transactionId, idempotencyKey, command);
        return jpa.save(row);
    }

    public boolean existsByTransactionId(String transactionId) {
        return transactionId != null && jpa.findByTransactionId(transactionId).isPresent();
    }

    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    public Optional<PaymentTransaction> findByTransactionId(String transactionId) {
        return jpa.findByTransactionId(transactionId);
    }

    public List<PaymentTransaction> findHistoryForUser(Long userId, int limit) {
        return jpa.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
                userId, userId, PageRequest.of(0, limit));
    }

    public boolean updateStatusIfPending(String transactionId, TransactionStatus newStatus) {
        return jpa.updateStatusIfPending(transactionId, newStatus, TransactionStatus.PENDING) > 0;
    }
}
