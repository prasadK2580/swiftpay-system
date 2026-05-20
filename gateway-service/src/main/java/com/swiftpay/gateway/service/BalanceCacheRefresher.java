package com.swiftpay.gateway.service;

import com.swiftpay.gateway.port.BalanceStore;
import com.swiftpay.gateway.port.LedgerBalanceReader;
import org.springframework.stereotype.Component;

@Component
public class BalanceCacheRefresher {

    private final LedgerBalanceReader accountBalanceReader;
    private final BalanceStore balanceStore;

    public BalanceCacheRefresher(LedgerBalanceReader accountBalanceReader, BalanceStore balanceStore) {
        this.accountBalanceReader = accountBalanceReader;
        this.balanceStore = balanceStore;
    }


    public void refreshSenderBalanceFromLedger(Long senderId, String currency) {
        accountBalanceReader.getBalance(senderId, currency)
                .ifPresent(balance -> balanceStore.setBalance(senderId, balance));
    }
}
