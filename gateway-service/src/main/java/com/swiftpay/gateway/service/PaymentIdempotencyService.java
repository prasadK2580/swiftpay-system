package com.swiftpay.gateway.service;

import com.swiftpay.gateway.model.IdempotencyCheckResult;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.cache.PaymentIdempotencyCache;
import com.swiftpay.gateway.repository.PaymentRepository;
import com.swiftpay.shared.exception.IdempotencyConflictException;
import org.springframework.stereotype.Service;

@Service
public class PaymentIdempotencyService {

    private final PaymentIdempotencyCache idempotencyCache;
    private final PaymentRepository paymentRepository;

    public PaymentIdempotencyService(PaymentIdempotencyCache idempotencyCache, PaymentRepository paymentRepository) {
        this.idempotencyCache = idempotencyCache;
        this.paymentRepository = paymentRepository;
    }


    public void requireNewPaymentLock(String idempotencyKey, PaymentCommand command) {
        requireNewPaymentLock(idempotencyKey, command, false);
    }

    private void requireNewPaymentLock(String idempotencyKey, PaymentCommand command, 
        boolean retryAfterStaleLock) {
        IdempotencyCheckResult outcome = idempotencyCache.lockIdempotencyKeyOrReturnExisting(idempotencyKey, command);

        if (outcome.isTransactionAlreadyProcessed()) {
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
                idempotencyCache.deleteIdempotencyKey(idempotencyKey);
                requireNewPaymentLock(idempotencyKey, command, true);
                return;
            }
            throw new IllegalStateException("Could not acquire idempotency lock for key=" + idempotencyKey);
        }
    }

    public void recordValidationFailure(String idempotencyKey, PaymentCommand command, String errorMessage) {
        idempotencyCache.saveFailedIdempotencyState(idempotencyKey, null, command, errorMessage);
    }

    private boolean paymentExists(String transactionId, String idempotencyKey) {
        return paymentRepository.existsByTransactionId(transactionId)
                || paymentRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
