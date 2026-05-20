package com.swiftpay.ledger.service;

import com.swiftpay.ledger.controller.dto.TransactionHistoryItem;
import com.swiftpay.ledger.port.AccountReader;
import com.swiftpay.ledger.port.TransactionReader;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionHistoryService implements TransactionHistoryUseCase {


    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private final AccountReader accountReader;
    private final TransactionReader transactionReader;

    public TransactionHistoryService(AccountReader accountReader, TransactionReader transactionReader) {
        this.accountReader = accountReader;
        this.transactionReader = transactionReader;
    }

    @Override
    public List<TransactionHistoryItem> getHistoryForUser(Long userId, int limit) {
        if (!accountReader.existsByUserId(userId)) {
            throw new UserNotFoundException(userId);
        }
        int effectiveLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return transactionReader.findHistoryForUser(userId, effectiveLimit).stream()
                .map(TransactionHistoryItem::from)
                .toList();
    }
}
