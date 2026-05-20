package com.swiftpay.ledger.infrastructure.persistence;

import com.swiftpay.ledger.port.AccountReader;
import com.swiftpay.ledger.repo.AccountRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountReader implements AccountReader {

    private final AccountRepository accountRepository;

    public JpaAccountReader(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @Override
    public boolean existsByUserId(Long userId) {
        return accountRepository.existsById(userId);
    }
}
