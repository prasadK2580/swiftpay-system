package com.swiftpay.gateway.application.validation;

import com.swiftpay.gateway.model.PaymentCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentBusinessRulesValidatorTest {

    private final PaymentBusinessRulesValidator validator = new PaymentBusinessRulesValidator();

    @Test
    void validate_acceptsDifferentSenderAndReceiver() {
        PaymentCommand command = new PaymentCommand(1001L, 2002L, 10.0, "INR");
        assertDoesNotThrow(() -> validator.validate(command));
    }

    @Test
    void validate_rejectsSameSenderAndReceiver() {
        PaymentCommand command = new PaymentCommand(1001L, 1001L, 10.0, "INR");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }
}
