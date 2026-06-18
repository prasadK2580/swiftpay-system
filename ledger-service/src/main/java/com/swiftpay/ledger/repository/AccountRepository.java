package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.model.SettlementAccount;

import java.util.Optional;

/** Account reads and settlement balance updates (PostgreSQL). */
public interface AccountRepository {

    boolean existsByUserId(Long userId);

    Optional<SettlementAccount> findById(Long userId);

    Optional<SettlementAccount> findByIdForUpdate(Long userId);

    void save(SettlementAccount account);
}
