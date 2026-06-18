package com.swiftpay.ledger.service.impl;

import com.swiftpay.ledger.event.SettlementEventProducer;
import com.swiftpay.ledger.model.SettlementResult;
import com.swiftpay.ledger.service.PaymentSettlementProcessor;
import com.swiftpay.ledger.service.AccountBalanceRedisUpdater;
import com.swiftpay.ledger.service.SettlementService;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.stereotype.Service;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final PaymentSettlementProcessor paymentSettlementProcessor;
    private final AccountBalanceRedisUpdater accountBalanceRedisUpdater;
    private final SettlementEventProducer settlementEventProducer;

    public SettlementServiceImpl(
            PaymentSettlementProcessor paymentSettlementProcessor,
            AccountBalanceRedisUpdater accountBalanceRedisUpdater,
            SettlementEventProducer settlementEventProducer) {
        this.paymentSettlementProcessor = paymentSettlementProcessor;
        this.accountBalanceRedisUpdater = accountBalanceRedisUpdater;
        this.settlementEventProducer = settlementEventProducer;
    }

    @Override
    public void settle(PaymentInitiatedEvent event) {
        SettlementResult result = paymentSettlementProcessor.settlePayment(event);
        accountBalanceRedisUpdater.syncAfterSettlement(event, result);
        publishOutcome(event, result);
    }

    private void publishOutcome(PaymentInitiatedEvent event, SettlementResult result) {
        switch (result.outcome()) {
            case COMPLETED -> settlementEventProducer.publishPaymentCompleted(
                    PaymentCompletedEvent.from(event));
            case FAILED -> settlementEventProducer.publishPaymentFailed(
                    PaymentFailedEvent.from(event, result.failureReason()));
            case SKIPPED -> { }
        }
    }
}
