package com.swiftpay.ledger.infrastructure.kafka;

import com.swiftpay.ledger.service.SettlementService;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ledger.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentInitiatedKafkaListener {

    private final SettlementService settlementService;

    public PaymentInitiatedKafkaListener(SettlementService settlementService) {
        this.settlementService = settlementService;
    }


    @KafkaListener(
            topics = "${app.kafka.topics.payment-initiated}",
            groupId = "${app.ledger.consumer.group-id}",
            containerFactory = "paymentInitiatedKafkaListenerContainerFactory")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        settlementService.settle(event);
    }
}
