package com.swiftpay.gateway.service;

import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.model.PaymentCommand;

public interface PaymentInitiationUseCase {

    PaymentResponse initiatePayment(String idempotencyKey, PaymentCommand command);
}
