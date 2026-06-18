package com.swiftpay.gateway.service;

import com.swiftpay.gateway.cache.PaymentBalanceCache;
import com.swiftpay.gateway.cache.PaymentStatusCache;
import com.swiftpay.gateway.client.LedgerBalanceClient;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentRedisCacheUpdater {

    private final PaymentStatusCache paymentStatusCache;
    private final PaymentBalanceCache paymentBalanceCache;
    private final LedgerBalanceClient ledgerBalanceClient;

    public PaymentRedisCacheUpdater(
            PaymentStatusCache paymentStatusCache,
            PaymentBalanceCache paymentBalanceCache,
            LedgerBalanceClient ledgerBalanceClient) {
        this.paymentStatusCache = paymentStatusCache;
        this.paymentBalanceCache = paymentBalanceCache;
        this.ledgerBalanceClient = ledgerBalanceClient;
    }

    public void updateRedisAfterPaymentCompleted(PaymentCompletedEvent event) {
        paymentStatusCache.writeStatus(event.getTransactionId(), TransactionStatus.COMPLETED);
        refreshBalanceFromLedger(event.getSenderId(), event.getCurrency());
        refreshBalanceFromLedger(event.getReceiverId(), event.getCurrency());
    }

    public void updateRedisAfterPaymentFailed(PaymentFailedEvent event) {
        paymentStatusCache.writeStatus(event.getTransactionId(), TransactionStatus.FAILED);
        refreshBalanceFromLedger(event.getSenderId(), event.getCurrency());
    }

    private void refreshBalanceFromLedger(Long userId, String currency) {
        if (userId == null || currency == null) {
            return;
        }
        ledgerBalanceClient.getBalance(userId, currency)
                .ifPresent(balance -> paymentBalanceCache.setBalance(userId, balance));
    }
}
