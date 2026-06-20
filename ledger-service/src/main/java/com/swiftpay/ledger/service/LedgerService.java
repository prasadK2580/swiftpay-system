package com.swiftpay.ledger.service;

import com.swiftpay.ledger.controller.dto.LedgerBalanceResponse;
import com.swiftpay.ledger.controller.dto.TransactionHistoryItem;

import java.util.List;

public interface LedgerService {

    LedgerBalanceResponse getBalance(Long userId, String currency);

    List<TransactionHistoryItem> getHistoryForUser(Long userId, int limit);
}
