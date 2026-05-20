package com.swiftpay.gateway.application.validation;

import com.swiftpay.gateway.model.PaymentCommand;

/**
 * Single-responsibility validation step (OCP — add new validators without changing orchestration).
 */
public interface PaymentValidator {

    void validate(PaymentCommand command);
}
