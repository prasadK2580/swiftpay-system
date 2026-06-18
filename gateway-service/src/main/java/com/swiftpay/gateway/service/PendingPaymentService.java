package com.swiftpay.gateway.service;

import com.swiftpay.gateway.application.mapper.PaymentInitiatedEventMapper;
import com.swiftpay.gateway.cache.DuplicatePaymentChecker;
import com.swiftpay.gateway.cache.PaymentIdempotencyCache;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.event.PaymentEventProducer;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PendingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyCache idempotencyCache;
    private final DuplicatePaymentChecker duplicatePaymentChecker;
    private final PaymentEventProducer paymentEventProducer;

    public PendingPaymentService(
            PaymentRepository paymentRepository,
            PaymentIdempotencyCache idempotencyCache,
            DuplicatePaymentChecker duplicatePaymentChecker,
            PaymentEventProducer paymentEventProducer) {
        this.paymentRepository = paymentRepository;
        this.idempotencyCache = idempotencyCache;
        this.duplicatePaymentChecker = duplicatePaymentChecker;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public PaymentResponse savePendingPaymentAndPublishEvent(
            String transactionId, String idempotencyKey, PaymentCommand command) {

        PaymentResponse response = PaymentResponse.from(
                paymentRepository.savePending(transactionId, idempotencyKey, command));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    idempotencyCache.saveCompletedIdempotencyState(idempotencyKey, transactionId, command);
                    paymentEventProducer.publishPaymentInitiated(PaymentInitiatedEventMapper.from(response));
                } catch (Exception ex) {
                    log.error("post-commit failed tx={}", transactionId, ex);
                    idempotencyCache.saveFailedIdempotencyState(
                            idempotencyKey, transactionId, command, ex.getMessage());
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    idempotencyCache.deleteIdempotencyKey(idempotencyKey);
                    duplicatePaymentChecker.removeTransactionIdReservation(transactionId);
                }
            }
        });

        return response;
    }
}
