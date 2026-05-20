package com.swiftpay.gateway.application.mapper;

import com.swiftpay.gateway.controller.dto.PaymentRequest;
import com.swiftpay.gateway.model.PaymentCommand;

public final class PaymentCommandMapper {

    private PaymentCommandMapper() {
    }

    public static PaymentCommand fromRequest(PaymentRequest request) {
        return new PaymentCommand(
                request.getSenderId(),
                request.getReceiverId(),
                request.getAmount(),
                request.getCurrency());
    }
}
