package com.swiftpay.gateway.application.idempotency;
public class IdempotencyOutcome {

    public enum Type {
        PROCEED,
        ALREADY_PROCESSED,
        CONFLICT,
        IN_PROGRESS
    }

    private final Type type;
    private final String transactionId;

    private IdempotencyOutcome(Type type, String transactionId) {
        this.type = type;
        this.transactionId = transactionId;
    }

    public static IdempotencyOutcome proceed() {
        return new IdempotencyOutcome(Type.PROCEED, null);
    }

    public static IdempotencyOutcome alreadyProcessed(String transactionId) {
        return new IdempotencyOutcome(Type.ALREADY_PROCESSED, transactionId);
    }

    public static IdempotencyOutcome conflict() {
        return new IdempotencyOutcome(Type.CONFLICT, null);
    }

    public static IdempotencyOutcome inProgress(String transactionId) {
        return new IdempotencyOutcome(Type.IN_PROGRESS, transactionId);
    }

    public Type getType() {
        return type;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isProceed() {
        return type == Type.PROCEED;
    }

    public boolean isAlreadyProcessed() {
        return type == Type.ALREADY_PROCESSED;
    }

    public boolean isConflict() {
        return type == Type.CONFLICT;
    }

    public boolean isInProgress() {
        return type == Type.IN_PROGRESS;
    }
}
