package com.swiftpay.ledger.service;

import com.swiftpay.shared.event.PaymentInitiatedEvent;

public interface SettlementService {

    void settle(PaymentInitiatedEvent event);
}
