package com.swiftpay.gateway.model;
public class IdempotencyCheckResult {

    public enum Type {
        PROCEED,
        ALREADY_PROCESSED,
        CONFLICT,
        IN_PROGRESS
    }

    private final Type type;
    private final String transactionId;

    private IdempotencyCheckResult(Type type, String transactionId) {
        this.type = type;
        this.transactionId = transactionId;
    }

    public static IdempotencyCheckResult proceed() {
        return new IdempotencyCheckResult(Type.PROCEED, null);
    }

    public static IdempotencyCheckResult alreadyProcessed(String transactionId) {
        return new IdempotencyCheckResult(Type.ALREADY_PROCESSED, transactionId);
    }

    public static IdempotencyCheckResult conflict() {
        return new IdempotencyCheckResult(Type.CONFLICT, null);
    }

    public static IdempotencyCheckResult inProgress(String transactionId) {
        return new IdempotencyCheckResult(Type.IN_PROGRESS, transactionId);
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

    public boolean isTransactionAlreadyProcessed() {
        return type == Type.ALREADY_PROCESSED;
    }

    public boolean isConflict() {
        return type == Type.CONFLICT;
    }

    public boolean isInProgress() {
        return type == Type.IN_PROGRESS;
    }
}
