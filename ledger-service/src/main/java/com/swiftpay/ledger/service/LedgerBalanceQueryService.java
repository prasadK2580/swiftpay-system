package com.swiftpay.ledger.service;

import com.swiftpay.ledger.controller.dto.LedgerBalanceResponse;
import com.swiftpay.ledger.port.SettlementAccountStore;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LedgerBalanceQueryService {

    private final SettlementAccountStore accountStore;

    public LedgerBalanceQueryService(SettlementAccountStore accountStore) {
        this.accountStore = accountStore;
    }


    public LedgerBalanceResponse getBalance(Long userId, String currency) {
        return accountStore.findById(userId)
                .filter(account -> account.currency().equalsIgnoreCase(currency))
                .map(account -> new LedgerBalanceResponse(userId, account.balance(), account.currency()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
