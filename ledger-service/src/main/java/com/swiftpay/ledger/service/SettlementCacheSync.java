package com.swiftpay.ledger.service;

import com.swiftpay.ledger.model.SettlementOutcome;
import com.swiftpay.ledger.model.SettlementResult;
import com.swiftpay.ledger.port.BalanceStore;
import com.swiftpay.ledger.port.SettlementAccountStore;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SettlementCacheSync {

    private static final Logger log = LoggerFactory.getLogger(SettlementCacheSync.class);

    private final SettlementAccountStore accountStore;
    private final BalanceStore balanceStore;

    public SettlementCacheSync(SettlementAccountStore accountStore, BalanceStore balanceStore) {
        this.accountStore = accountStore;
        this.balanceStore = balanceStore;
    }


    public void syncAfterSettlement(PaymentInitiatedEvent event, SettlementResult result) {
        if (result.outcome() == SettlementOutcome.SKIPPED) {
            return;
        }
        refreshBalance(event.getSenderId(), event.getCurrency());
        refreshBalance(event.getReceiverId(), event.getCurrency());
    }

    private void refreshBalance(Long userId, String currency) {
        if (userId == null || currency == null) {
            return;
        }
        try {
            accountStore.findById(userId)
                    .filter(account -> account.currency().equalsIgnoreCase(currency))
                    .ifPresent(account -> balanceStore.setBalance(userId, account.balance()));
        } catch (Exception ex) {
            log.warn("Redis balance sync failed userId={}", userId, ex);
        }
    }
}
