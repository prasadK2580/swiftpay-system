package com.swiftpay.gateway.service;

import com.swiftpay.gateway.application.validation.IdempotencyKeyValidator;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.TransactionDeduplicationGuard;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentInitiationService implements PaymentInitiationUseCase {

    private final IdempotencyKeyValidator idempotencyKeyValidator;
    private final IdempotencyCoordinator idempotencyCoordinator;
    private final PaymentPrePersistValidator paymentPrePersistValidator;
    private final TransactionDeduplicationGuard transactionDeduplicationGuard;
    private final PaymentPersistenceService paymentPersistenceService;

    public PaymentInitiationService(IdempotencyKeyValidator idempotencyKeyValidator, IdempotencyCoordinator idempotencyCoordinator, PaymentPrePersistValidator paymentPrePersistValidator, TransactionDeduplicationGuard transactionDeduplicationGuard, PaymentPersistenceService paymentPersistenceService) {
        this.idempotencyKeyValidator = idempotencyKeyValidator;
        this.idempotencyCoordinator = idempotencyCoordinator;
        this.paymentPrePersistValidator = paymentPrePersistValidator;
        this.transactionDeduplicationGuard = transactionDeduplicationGuard;
        this.paymentPersistenceService = paymentPersistenceService;
    }


    @Override
    public PaymentResponse initiatePayment(String idempotencyKey, PaymentCommand command) {
        String key = idempotencyKeyValidator.validateAndNormalize(idempotencyKey);
        idempotencyCoordinator.ensureLockAcquired(key, command);
        paymentPrePersistValidator.validate(key, command);

        String transactionId = UUID.randomUUID().toString();
        transactionDeduplicationGuard.reserveBeforePersist(transactionId);

        return paymentPersistenceService.savePendingWithIdempotencyAfterCommit(transactionId, key, command);
    }
}
