package com.swiftpay.ledger.port;

import com.swiftpay.ledger.entity.PaymentTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Port for read-only transaction access (DIP, ISP).
 */
public interface TransactionReader {

    boolean existsByTransactionId(String transactionId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findHistoryForUser(Long userId, int limit);
}
