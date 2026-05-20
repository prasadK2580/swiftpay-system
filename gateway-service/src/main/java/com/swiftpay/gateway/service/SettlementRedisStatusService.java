package com.swiftpay.gateway.service;

import com.swiftpay.gateway.port.BalanceStore;
import com.swiftpay.gateway.port.LedgerBalanceReader;
import com.swiftpay.gateway.port.TransactionStatusCache;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class SettlementRedisStatusService {

    private final TransactionStatusCache transactionStatusCache;
    private final BalanceStore balanceStore;
    private final LedgerBalanceReader ledgerBalanceReader;

    public SettlementRedisStatusService(TransactionStatusCache transactionStatusCache, BalanceStore balanceStore, LedgerBalanceReader ledgerBalanceReader) {
        this.transactionStatusCache = transactionStatusCache;
        this.balanceStore = balanceStore;
        this.ledgerBalanceReader = ledgerBalanceReader;
    }


    public void syncAfterCompleted(PaymentCompletedEvent event) {
        transactionStatusCache.writeStatus(event.getTransactionId(), TransactionStatus.COMPLETED);
        refreshBalanceFromLedger(event.getSenderId(), event.getCurrency());
        refreshBalanceFromLedger(event.getReceiverId(), event.getCurrency());
    }

    public void syncAfterFailed(PaymentFailedEvent event) {
        transactionStatusCache.writeStatus(event.getTransactionId(), TransactionStatus.FAILED);
        refreshBalanceFromLedger(event.getSenderId(), event.getCurrency());
    }

    private void refreshBalanceFromLedger(Long userId, String currency) {
        if (userId == null || currency == null) {
            return;
        }
        ledgerBalanceReader.getBalance(userId, currency)
                .ifPresent(balance -> balanceStore.setBalance(userId, balance));
    }
}
