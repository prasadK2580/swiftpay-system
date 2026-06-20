package com.swiftpay.gateway.service;

import com.swiftpay.gateway.repository.PaymentRepository;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import com.swiftpay.shared.event.PaymentCompletedEvent;
import com.swiftpay.shared.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentStatusUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusUpdateService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentRedisCacheUpdater paymentRedisCacheUpdater;

    public PaymentStatusUpdateService(
            PaymentRepository paymentRepository,
            PaymentRedisCacheUpdater paymentRedisCacheUpdater) {
        this.paymentRepository = paymentRepository;
        this.paymentRedisCacheUpdater = paymentRedisCacheUpdater;
    }

    @Transactional
    public void applyPaymentCompleted(PaymentCompletedEvent event) {
        paymentRepository.updateStatusIfPending(event.getTransactionId(), TransactionStatus.COMPLETED);
        paymentRedisCacheUpdater.updateRedisAfterPaymentCompleted(event);
    }

    @Transactional
    public void applyPaymentFailed(PaymentFailedEvent event) {
        log.warn("payment failed tx={} reason={}", event.getTransactionId(), event.getReason());
        paymentRepository.updateStatusIfPending(event.getTransactionId(), TransactionStatus.FAILED);
        paymentRedisCacheUpdater.updateRedisAfterPaymentFailed(event);
    }
}
