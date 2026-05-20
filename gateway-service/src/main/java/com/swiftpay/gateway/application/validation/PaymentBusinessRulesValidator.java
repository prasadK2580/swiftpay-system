package com.swiftpay.gateway.application.validation;

import com.swiftpay.gateway.model.PaymentCommand;
import org.springframework.stereotype.Component;

@Component
public class PaymentBusinessRulesValidator implements PaymentValidator {

    @Override
    public void validate(PaymentCommand command) {
        if (command.senderId().equals(command.receiverId())) {
            throw new IllegalArgumentException("senderId and receiverId must be different");
        }
    }
}
