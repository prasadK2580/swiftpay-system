package com.swiftpay.ledger.port;
/**
 * Port for account existence checks (DIP).
 */
public interface AccountReader {

    boolean existsByUserId(Long userId);
}
