package com.swiftpay.gateway.service;

import com.swiftpay.gateway.application.idempotency.IdempotencyOutcome;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.IdempotencyGuard;
import com.swiftpay.gateway.port.TransactionReader;
import com.swiftpay.shared.exception.IdempotencyConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCoordinator {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCoordinator.class);

    private final IdempotencyGuard idempotencyGuard;
    private final TransactionReader transactionReader;

    public IdempotencyCoordinator(IdempotencyGuard idempotencyGuard, TransactionReader transactionReader) {
        this.idempotencyGuard = idempotencyGuard;
        this.transactionReader = transactionReader;
    }


    public void ensureLockAcquired(String idempotencyKey, PaymentCommand command) {
        ensureLockAcquired(idempotencyKey, command, false);
    }

    private void ensureLockAcquired(String idempotencyKey, PaymentCommand command, boolean retryAfterStaleLock) {
        IdempotencyOutcome outcome = idempotencyGuard.checkAndLock(idempotencyKey, command);

        if (outcome.isAlreadyProcessed()) {
            throw IdempotencyConflictException.alreadyProcessed(idempotencyKey);
        }
        if (outcome.isConflict()) {
            throw IdempotencyConflictException.requestMismatch(idempotencyKey);
        }
        if (outcome.isInProgress()) {
            if (paymentExists(outcome.getTransactionId(), idempotencyKey)) {
                throw IdempotencyConflictException.alreadyProcessed(idempotencyKey);
            }
            if (!retryAfterStaleLock) {
                log.warn("Stale idempotency lock, retrying key={}", idempotencyKey);
                idempotencyGuard.releaseLock(idempotencyKey);
                ensureLockAcquired(idempotencyKey, command, true);
                return;
            }
            throw new IllegalStateException("Could not acquire idempotency lock for key=" + idempotencyKey);
        }
    }

    public void markValidationFailed(String idempotencyKey, PaymentCommand command, String errorMessage) {
        idempotencyGuard.markFailed(idempotencyKey, null, command, errorMessage);
    }

    private boolean paymentExists(String transactionId, String idempotencyKey) {
        return transactionReader.existsByTransactionId(transactionId)
                || transactionReader.existsByIdempotencyKey(idempotencyKey);
    }
}
