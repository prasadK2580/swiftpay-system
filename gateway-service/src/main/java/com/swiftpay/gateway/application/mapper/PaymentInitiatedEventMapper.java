package com.swiftpay.gateway.application.mapper;

import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.shared.event.PaymentInitiatedEvent;

public final class PaymentInitiatedEventMapper {

    private PaymentInitiatedEventMapper() {
    }

    public static PaymentInitiatedEvent from(PaymentTransaction transaction) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent();
        event.setEventType(PaymentInitiatedEvent.EVENT_TYPE);
        event.setIdempotencyKey(transaction.getIdempotencyKey());
        event.setTransactionId(transaction.getTransactionId());
        event.setSenderId(transaction.getSenderId());
        event.setReceiverId(transaction.getReceiverId());
        event.setAmount(transaction.getAmount());
        event.setCurrency(transaction.getCurrency());
        event.setStatus(transaction.getStatus());
        event.setOccurredAt(transaction.getCreatedAt());
        return event;
    }

    public static PaymentInitiatedEvent from(PaymentResponse response) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent();
        event.setEventType(PaymentInitiatedEvent.EVENT_TYPE);
        event.setIdempotencyKey(response.getIdempotencyKey());
        event.setTransactionId(response.getTransactionId());
        event.setSenderId(response.getSenderId());
        event.setReceiverId(response.getReceiverId());
        event.setAmount(response.getAmount());
        event.setCurrency(response.getCurrency());
        event.setStatus(response.getStatus());
        event.setOccurredAt(response.getCreatedAt());
        return event;
    }
}
