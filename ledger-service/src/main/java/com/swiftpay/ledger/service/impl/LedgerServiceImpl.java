package com.swiftpay.ledger.service.impl;

import com.swiftpay.ledger.controller.dto.LedgerBalanceResponse;
import com.swiftpay.ledger.controller.dto.TransactionHistoryItem;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.PaymentHistoryRepository;
import com.swiftpay.ledger.service.LedgerService;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerServiceImpl implements LedgerService {

    public static final int MAX_LIMIT = 200;

    private final AccountRepository accountRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public LedgerServiceImpl(
            AccountRepository accountRepository,
            PaymentHistoryRepository paymentHistoryRepository) {
        this.accountRepository = accountRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @Override
    public LedgerBalanceResponse getBalance(Long userId, String currency) {
        return accountRepository.findById(userId)
                .filter(account -> account.currency().equalsIgnoreCase(currency))
                .map(account -> new LedgerBalanceResponse(userId, account.balance(), account.currency()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<TransactionHistoryItem> getHistoryForUser(Long userId, int limit) {
        if (!accountRepository.existsByUserId(userId)) {
            throw new UserNotFoundException(userId);
        }
        int effectiveLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return paymentHistoryRepository.findHistoryForUser(userId, effectiveLimit).stream()
                .map(TransactionHistoryItem::from)
                .toList();
    }
}
