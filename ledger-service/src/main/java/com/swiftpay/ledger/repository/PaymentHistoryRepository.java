package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.PaymentTransaction;

import java.util.List;

/** Read-only payment history for an account. */
public interface PaymentHistoryRepository {

    List<PaymentTransaction> findHistoryForUser(Long userId, int limit);
}
