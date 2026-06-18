package com.swiftpay.gateway.event;

import com.swiftpay.shared.domain.LedgerLockOrdering;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publishes {@link PaymentInitiatedEvent} to Kafka when {@code app.kafka.enabled=true};
 * otherwise this method is a no-op (local/tests without Kafka).
 */
@Component
public class PaymentEventProducer {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate;
    private final String paymentInitiatedTopic;
    private final boolean kafkaEnabled;

    public PaymentEventProducer(
            @Autowired(required = false) KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.payment-initiated}") String paymentInitiatedTopic,
            @Value("${app.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentInitiatedTopic = paymentInitiatedTopic;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            return;
        }
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
