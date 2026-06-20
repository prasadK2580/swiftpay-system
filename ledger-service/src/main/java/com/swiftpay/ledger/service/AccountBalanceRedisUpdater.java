package com.swiftpay.ledger.service;

import com.swiftpay.ledger.model.SettlementOutcome;
import com.swiftpay.ledger.model.SettlementResult;
import com.swiftpay.ledger.cache.AccountBalanceCache;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.stereotype.Component;

@Component
public class AccountBalanceRedisUpdater {

    private final AccountRepository accountRepository;
    private final AccountBalanceCache balanceStore;

    public AccountBalanceRedisUpdater(AccountRepository accountRepository, AccountBalanceCache balanceStore) {
        this.accountRepository = accountRepository;
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
        accountRepository.findById(userId)
                .filter(account -> account.currency().equalsIgnoreCase(currency))
                .ifPresent(account -> balanceStore.setBalance(userId, account.balance()));
    }
}
