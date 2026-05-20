package com.swiftpay.ledger.infrastructure.kafka;

import com.swiftpay.ledger.port.SettlementEventPublisher;
import com.swiftpay.shared.domain.LedgerLockOrdering;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaSettlementEventPublisher implements SettlementEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, PaymentCompletedEvent> completedKafkaTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> failedKafkaTemplate;
    private final String paymentCompletedTopic;
    private final String paymentFailedTopic;

    public KafkaSettlementEventPublisher(
            KafkaTemplate<String, PaymentCompletedEvent> completedKafkaTemplate,
            KafkaTemplate<String, PaymentFailedEvent> failedKafkaTemplate,
            @Value("${app.kafka.topics.payment-completed}") String paymentCompletedTopic,
            @Value("${app.kafka.topics.payment-failed}") String paymentFailedTopic) {
        this.completedKafkaTemplate = completedKafkaTemplate;
        this.failedKafkaTemplate = failedKafkaTemplate;
        this.paymentCompletedTopic = paymentCompletedTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String partitionKey = LedgerLockOrdering.kafkaPartitionKey(event.getSenderId(), event.getReceiverId());
        send(completedKafkaTemplate, paymentCompletedTopic, partitionKey, event);
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        String partitionKey = LedgerLockOrdering.kafkaPartitionKey(event.getSenderId(), event.getReceiverId());
        send(failedKafkaTemplate, paymentFailedTopic, partitionKey, event);
    }

    private <T> void send(KafkaTemplate<String, T> template, String topic, String partitionKey, T event) {
        try {
            SendResult<String, T> result = template.send(topic, partitionKey, event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result == null) {
                throw new IllegalStateException("Kafka send returned null");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish settlement event", ex);
        }
    }
}
