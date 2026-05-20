package com.swiftpay.ledger.service;

import com.swiftpay.ledger.model.SettlementResult;
import com.swiftpay.ledger.port.SettlementEventPublisher;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentInitiatedSettlementHandler {

    private final LedgerSettlementService ledgerSettlementService;
    private final SettlementCacheSync settlementCacheSync;
    private final SettlementEventPublisher settlementEventPublisher;

    public PaymentInitiatedSettlementHandler(LedgerSettlementService ledgerSettlementService, SettlementCacheSync settlementCacheSync, SettlementEventPublisher settlementEventPublisher) {
        this.ledgerSettlementService = ledgerSettlementService;
        this.settlementCacheSync = settlementCacheSync;
        this.settlementEventPublisher = settlementEventPublisher;
    }


    public void handle(PaymentInitiatedEvent event) {
        SettlementResult result = ledgerSettlementService.settlePayment(event);
        settlementCacheSync.syncAfterSettlement(event, result);
        publishOutcome(event, result);
    }

    private void publishOutcome(PaymentInitiatedEvent event, SettlementResult result) {
        switch (result.outcome()) {
            case COMPLETED -> settlementEventPublisher.publishPaymentCompleted(
                    PaymentCompletedEvent.from(event));
            case FAILED -> settlementEventPublisher.publishPaymentFailed(
                    PaymentFailedEvent.from(event, result.failureReason()));
            case SKIPPED -> { }
        }
    }
}
