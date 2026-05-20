package com.swiftpay.gateway.port;
import java.util.Optional;

/**
 * Reads authoritative balance from the ledger (accounts store).
 * Used only to refresh Redis before validation — not for the validation read itself.
 */
public interface LedgerBalanceReader {

    Optional<Double> getBalance(Long senderId, String currency);
}
