package com.swiftpay.ledger.cache;
import java.util.Optional;

/**
 * Port for reading (and optionally updating) cached account balances (DIP).
 */
public interface AccountBalanceCache {

  Optional<Double> getBalance(Long senderId, String currency);

    void setBalance(Long senderId, double balance);
}
