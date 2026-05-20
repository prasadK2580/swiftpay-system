package com.swiftpay.gateway.application.validation;
import com.swiftpay.shared.exception.MissingIdempotencyKeyException;
import org.springframework.stereotype.Component;

/**
 * Validates idempotency header values (SRP).
 */
@Component
public class IdempotencyKeyValidator {

    public String validateAndNormalize(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        return idempotencyKey.trim();
    }
}
