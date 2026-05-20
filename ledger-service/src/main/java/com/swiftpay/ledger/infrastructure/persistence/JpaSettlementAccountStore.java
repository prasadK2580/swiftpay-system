package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.model.SettlementAccount;
import com.swiftpay.ledger.port.SettlementAccountStore;
import com.swiftpay.ledger.repo.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaSettlementAccountStore implements SettlementAccountStore {

    private final AccountRepository accountRepository;

    public JpaSettlementAccountStore(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @Override
    public Optional<SettlementAccount> findById(Long userId) {
        return accountRepository.findById(userId).map(this::toModel);
    }

    @Override
    public Optional<SettlementAccount> findByIdForUpdate(Long userId) {
        return accountRepository.findByIdForUpdate(userId).map(this::toModel);
    }

    @Override
    public void save(SettlementAccount account) {
        Account entity = accountRepository.findById(account.userId())
                .orElseThrow(() -> new IllegalStateException("Account not found: " + account.userId()));
        entity.setBalance(account.balance());
        entity.setCurrency(account.currency());
        accountRepository.save(entity);
    }

    private SettlementAccount toModel(Account entity) {
        return new SettlementAccount(entity.getUserId(), entity.getBalance(), entity.getCurrency());
    }
}
