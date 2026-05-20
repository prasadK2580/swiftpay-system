package com.swiftpay.gateway.service;

import com.swiftpay.gateway.port.TransactionStatusWriter;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionStatusUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TransactionStatusUpdateService.class);

    private final TransactionStatusWriter transactionStatusWriter;
    private final SettlementRedisStatusService settlementRedisStatusService;

    public TransactionStatusUpdateService(TransactionStatusWriter transactionStatusWriter, SettlementRedisStatusService settlementRedisStatusService) {
        this.transactionStatusWriter = transactionStatusWriter;
        this.settlementRedisStatusService = settlementRedisStatusService;
    }


    @Transactional
    public void applyPaymentCompleted(PaymentCompletedEvent event) {
        transactionStatusWriter.updateStatusIfPending(event.getTransactionId(), TransactionStatus.COMPLETED);
        settlementRedisStatusService.syncAfterCompleted(event);
    }

    @Transactional
    public void applyPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed transactionId={} reason={}", event.getTransactionId(), event.getReason());
        transactionStatusWriter.updateStatusIfPending(event.getTransactionId(), TransactionStatus.FAILED);
        settlementRedisStatusService.syncAfterFailed(event);
    }
}
