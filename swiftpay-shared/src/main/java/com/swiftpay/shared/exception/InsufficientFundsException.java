package com.swiftpay.shared.exception;
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long senderId, String currency) {
        super("Insufficient funds for senderId=" + senderId + " in currency " + currency);
    }

    public InsufficientFundsException(Long senderId, String currency, String reason) {
        super("Insufficient funds for senderId=" + senderId + " in currency " + currency + ": " + reason);
    }

    public InsufficientFundsException(Long senderId, String currency, double available, double required) {
        super("Insufficient funds for senderId=" + senderId + " in currency " + currency
                + " (available=" + available + ", required=" + required + ")");
    }
}
