package com.swiftpay.ledger.port;

import com.swiftpay.ledger.model.SettlementAccount;

import java.util.Optional;

/**
 * Narrow persistence port for account debit/credit (ISP, DIP).
 */
public interface SettlementAccountStore {

    Optional<SettlementAccount> findById(Long userId);

    Optional<SettlementAccount> findByIdForUpdate(Long userId);

    void save(SettlementAccount account);
}
