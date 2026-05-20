package com.swiftpay.gateway.port;

import com.swiftpay.gateway.application.idempotency.IdempotencyOutcome;
import com.swiftpay.gateway.model.PaymentCommand;

/**
 * Port for idempotency locking and state in a fast store (DIP).
 */
public interface IdempotencyGuard {

    IdempotencyOutcome checkAndLock(String idempotencyKey, PaymentCommand command);

    void markCompleted(String idempotencyKey, String transactionId, PaymentCommand command);

    void releaseLock(String idempotencyKey);

    void markFailed(String idempotencyKey, String transactionId, PaymentCommand command, String errorMessage);
}
