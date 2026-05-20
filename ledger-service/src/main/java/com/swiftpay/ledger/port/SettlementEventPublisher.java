package com.swiftpay.ledger.port;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;

/**
 * Service B — emits settlement outcomes for Analytics and Service A feedback.
 */
public interface SettlementEventPublisher {

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);
}
