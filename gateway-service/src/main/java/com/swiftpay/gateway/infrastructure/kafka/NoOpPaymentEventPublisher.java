package com.swiftpay.gateway.infrastructure.kafka;

import com.swiftpay.shared.event.PaymentInitiatedEvent;
import com.swiftpay.gateway.port.PaymentEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
public class NoOpPaymentEventPublisher implements PaymentEventPublisher {

    @Override
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        // Kafka disabled
    }
}
