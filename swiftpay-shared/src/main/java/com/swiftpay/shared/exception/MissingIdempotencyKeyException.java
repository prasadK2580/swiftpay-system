package com.swiftpay.shared.exception;
public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required");
    }
}
