package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.ledger.repository.PaymentHistoryRepository;
import com.swiftpay.ledger.repository.TransactionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentHistoryPostgresRepository implements PaymentHistoryRepository {

    private final TransactionJpaRepository jpa;

    public PaymentHistoryPostgresRepository(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<PaymentTransaction> findHistoryForUser(Long userId, int limit) {
        return jpa.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
                userId, userId, PageRequest.of(0, limit));
    }
}
