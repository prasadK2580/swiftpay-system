package com.swiftpay.ledger.infrastructure.kafka;
import com.swiftpay.ledger.port.SettlementEventPublisher;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
public class NoOpSettlementEventPublisher implements SettlementEventPublisher {

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
    }
}
