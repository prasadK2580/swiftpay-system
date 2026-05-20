package com.swiftpay.gateway.port;
import com.swiftpay.shared.event.PaymentInitiatedEvent;

/**
 * Service A — Kafka producer port. Emits {@link PaymentInitiatedEvent} so downstream
 * services (Ledger, notifications, etc.) know a new transaction is ready to process.
 */
public interface PaymentEventPublisher {

    /**
     * Publish after the payment row is committed (status PENDING).
     */
    void publishPaymentInitiated(PaymentInitiatedEvent event);
}
