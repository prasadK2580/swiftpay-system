package com.swiftpay.gateway.service;

import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.port.TransactionReader;
import com.swiftpay.shared.exception.TransactionNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PaymentQueryService implements PaymentQueryUseCase {

    private final TransactionReader transactionReader;

    public PaymentQueryService(TransactionReader transactionReader) {
        this.transactionReader = transactionReader;
    }

    @Override
    public PaymentResponse getByTransactionId(String transactionId) {
        return transactionReader.findByTransactionId(transactionId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
