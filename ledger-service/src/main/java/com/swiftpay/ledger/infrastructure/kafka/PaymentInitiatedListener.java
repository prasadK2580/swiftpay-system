package com.swiftpay.ledger.infrastructure.kafka;

import com.swiftpay.ledger.service.PaymentInitiatedSettlementHandler;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ledger.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentInitiatedListener {

    private final PaymentInitiatedSettlementHandler settlementHandler;

    public PaymentInitiatedListener(PaymentInitiatedSettlementHandler settlementHandler) {
        this.settlementHandler = settlementHandler;
    }


    @KafkaListener(
            topics = "${app.kafka.topics.payment-initiated}",
            groupId = "${app.ledger.consumer.group-id}",
            containerFactory = "paymentInitiatedKafkaListenerContainerFactory")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        settlementHandler.handle(event);
    }
}
