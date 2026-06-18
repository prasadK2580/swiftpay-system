package com.swiftpay.ledger.service;

import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentInitiatedMessageValidator {

    public void validate(PaymentInitiatedEvent event) {
        if (event == null || event.getTransactionId() == null) {
            throw new IllegalArgumentException("Invalid PaymentInitiated event");
        }
        if (!PaymentInitiatedEvent.EVENT_TYPE.equals(event.getEventType())) {
            throw new IllegalArgumentException("Unexpected event type: " + event.getEventType());
        }
        if (event.getSenderId() == null || event.getReceiverId() == null || event.getAmount() == null) {
            throw new IllegalArgumentException("Missing settlement fields on event");
        }
        if (event.getSenderId().equals(event.getReceiverId())) {
            throw new IllegalArgumentException("sender and receiver must differ");
        }
        if (event.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
