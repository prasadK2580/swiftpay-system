package com.swiftpay.ledger.event;

import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;

/**
 * Publishes settlement outcome events to Kafka.
 */
public interface SettlementEventProducer {

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);
}
