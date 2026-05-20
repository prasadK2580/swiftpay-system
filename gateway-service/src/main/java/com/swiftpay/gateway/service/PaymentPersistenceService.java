package com.swiftpay.gateway.service;

import com.swiftpay.gateway.application.mapper.PaymentInitiatedEventMapper;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.IdempotencyGuard;
import com.swiftpay.gateway.port.PaymentEventPublisher;
import com.swiftpay.gateway.port.TransactionDeduplicationGuard;
import com.swiftpay.gateway.port.TransactionWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PaymentPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PaymentPersistenceService.class);

    private final TransactionWriter transactionWriter;
    private final IdempotencyGuard idempotencyGuard;
    private final TransactionDeduplicationGuard transactionDeduplicationGuard;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentPersistenceService(TransactionWriter transactionWriter, IdempotencyGuard idempotencyGuard, TransactionDeduplicationGuard transactionDeduplicationGuard, PaymentEventPublisher paymentEventPublisher) {
        this.transactionWriter = transactionWriter;
        this.idempotencyGuard = idempotencyGuard;
        this.transactionDeduplicationGuard = transactionDeduplicationGuard;
        this.paymentEventPublisher = paymentEventPublisher;
    }


    @Transactional
    public PaymentResponse savePendingWithIdempotencyAfterCommit(
            String transactionId, String idempotencyKey, PaymentCommand command) {

        PaymentResponse response = PaymentResponse.from(
                transactionWriter.savePending(transactionId, idempotencyKey, command));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    idempotencyGuard.markCompleted(idempotencyKey, transactionId, command);
                    paymentEventPublisher.publishPaymentInitiated(
                            PaymentInitiatedEventMapper.from(response));
                    log.info("Payment accepted transactionId={} status=PENDING", transactionId);
                } catch (Exception ex) {
                    log.error("Payment saved but post-commit failed transactionId={}", transactionId, ex);
                    idempotencyGuard.markFailed(
                            idempotencyKey, transactionId, command, ex.getMessage());
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    idempotencyGuard.releaseLock(idempotencyKey);
                    transactionDeduplicationGuard.releaseReservation(transactionId);
                }
            }
        });

        return response;
    }
}
