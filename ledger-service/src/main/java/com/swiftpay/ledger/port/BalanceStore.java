package com.swiftpay.ledger.port;
import java.util.Optional;

/**
 * Port for reading (and optionally updating) cached account balances (DIP).
 */
public interface BalanceStore {

  Optional<Double> getBalance(Long senderId, String currency);

    void setBalance(Long senderId, double balance);
}
