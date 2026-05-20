package com.swiftpay.gateway.infrastructure.kafka;

import com.swiftpay.gateway.port.PaymentEventPublisher;
import com.swiftpay.shared.domain.LedgerLockOrdering;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate;
    private final String paymentInitiatedTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.payment-initiated}") String paymentInitiatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentInitiatedTopic = paymentInitiatedTopic;
    }

    @Override
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        try {
            String partitionKey = LedgerLockOrdering.kafkaPartitionKey(
                    event.getSenderId(), event.getReceiverId());
            SendResult<String, PaymentInitiatedEvent> result = kafkaTemplate
                    .send(paymentInitiatedTopic, partitionKey, event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result == null) {
                throw new IllegalStateException("No Kafka ack for transactionId=" + event.getTransactionId());
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to publish PaymentInitiated for transactionId=" + event.getTransactionId(), ex);
        }
    }
}
