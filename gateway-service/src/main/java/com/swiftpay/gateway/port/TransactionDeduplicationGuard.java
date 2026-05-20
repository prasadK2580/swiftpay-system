package com.swiftpay.gateway.port;

/**
 * 24-hour Redis guard so a {@code transaction_id} cannot be processed twice.
 */
public interface TransactionDeduplicationGuard {

    /**
     * Reserves the transaction id before persisting PENDING. Throws if already seen within TTL.
     */
    void reserveBeforePersist(String transactionId);

    /**
     * Confirms the transaction after a successful DB commit (24h retention).
     */
    void confirmProcessed(String transactionId);

    /**
     * Releases a reservation when the DB transaction rolls back.
     */
    void releaseReservation(String transactionId);

    /**
     * Fast path: true if this transaction id was already completed within the TTL window.
     */
    boolean isAlreadyProcessed(String transactionId);
}
