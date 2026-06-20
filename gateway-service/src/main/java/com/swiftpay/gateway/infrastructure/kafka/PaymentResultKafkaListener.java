package com.swiftpay.gateway.infrastructure.kafka;

import com.swiftpay.gateway.service.PaymentStatusUpdateService;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.gateway.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentResultKafkaListener {

    private final PaymentStatusUpdateService paymentStatusUpdateService;

    public PaymentResultKafkaListener(PaymentStatusUpdateService paymentStatusUpdateService) {
        this.paymentStatusUpdateService = paymentStatusUpdateService;
    }


    @KafkaListener(
            topics = "${app.kafka.topics.payment-completed}",
            groupId = "${app.gateway.consumer.group-id}",
            containerFactory = "paymentCompletedKafkaListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        paymentStatusUpdateService.applyPaymentCompleted(event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-failed}",
            groupId = "${app.gateway.consumer.group-id}",
            containerFactory = "paymentFailedKafkaListenerContainerFactory")
    public void onPaymentFailed(PaymentFailedEvent event) {
        paymentStatusUpdateService.applyPaymentFailed(event);
    }
}
