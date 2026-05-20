package com.swiftpay.ledger.model;
public record SettlementResult(SettlementOutcome outcome, String failureReason) {

    public static SettlementResult completed() {
        return new SettlementResult(SettlementOutcome.COMPLETED, null);
    }

    public static SettlementResult failed(String reason) {
        return new SettlementResult(SettlementOutcome.FAILED, reason);
    }

    public static SettlementResult skipped() {
        return new SettlementResult(SettlementOutcome.SKIPPED, null);
    }
}
