package com.swiftpay.ledger.service;

import com.swiftpay.ledger.controller.dto.TransactionHistoryItem;

import java.util.List;

/**
 * Application use case for user transaction history (ISP).
 */
public interface TransactionHistoryUseCase {

    List<TransactionHistoryItem> getHistoryForUser(Long userId, int limit);
}
