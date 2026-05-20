package com.swiftpay.shared.exception;
public class IdempotencyConflictException extends RuntimeException {

    public static IdempotencyConflictException alreadyProcessed(String idempotencyKey) {
        return new IdempotencyConflictException(
                "Idempotency-Key '" + idempotencyKey + "' was already processed within the last 24 hours");
    }

    public static IdempotencyConflictException requestMismatch(String idempotencyKey) {
        return new IdempotencyConflictException(
                "Idempotency-Key '" + idempotencyKey + "' was already used with a different payment request");
    }

    public static IdempotencyConflictException transactionAlreadyProcessed(String transactionId) {
        return new IdempotencyConflictException(
                "Transaction '" + transactionId + "' was already processed within the last 24 hours");
    }

    private IdempotencyConflictException(String message) {
        super(message);
    }
}
