package com.swiftpay.gateway.service;

import com.swiftpay.gateway.controller.dto.PaymentResponse;

public interface PaymentQueryUseCase {

    PaymentResponse getByTransactionId(String transactionId);
}
