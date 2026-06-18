package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.model.SettlementAccount;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.AccountJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountPostgresRepository implements AccountRepository {

    private final AccountJpaRepository jpa;

    public AccountPostgresRepository(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpa.existsById(userId);
    }

    @Override
    public Optional<SettlementAccount> findById(Long userId) {
        return jpa.findById(userId).map(this::toModel);
    }

    @Override
    public Optional<SettlementAccount> findByIdForUpdate(Long userId) {
        return jpa.findByIdForUpdate(userId).map(this::toModel);
    }

    @Override
    public void save(SettlementAccount account) {
        Account entity = jpa.findById(account.userId())
                .orElseThrow(() -> new IllegalStateException("Account not found: " + account.userId()));
        entity.setBalance(account.balance());
        entity.setCurrency(account.currency());
        jpa.save(entity);
    }

    private SettlementAccount toModel(Account entity) {
        return new SettlementAccount(entity.getUserId(), entity.getBalance(), entity.getCurrency());
    }
}
