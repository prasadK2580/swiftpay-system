package com.swiftpay.gateway.service;

import com.swiftpay.gateway.cache.DuplicatePaymentChecker;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.repository.PaymentRepository;
import com.swiftpay.gateway.service.validation.PaymentRequestValidator;
import com.swiftpay.shared.exception.TransactionNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Main payment API: create payment and read status.
 */
@Service
public class PaymentService {

    private final PaymentRequestValidator paymentRequestValidator;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final DuplicatePaymentChecker duplicatePaymentChecker;
    private final PendingPaymentService pendingPaymentService;
    private final PaymentRepository paymentRepository;

    public PaymentService(
            PaymentRequestValidator paymentRequestValidator,
            PaymentIdempotencyService paymentIdempotencyService,
            DuplicatePaymentChecker duplicatePaymentChecker,
            PendingPaymentService pendingPaymentService,
            PaymentRepository paymentRepository) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.paymentIdempotencyService = paymentIdempotencyService;
        this.duplicatePaymentChecker = duplicatePaymentChecker;
        this.pendingPaymentService = pendingPaymentService;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(String idempotencyKey, PaymentCommand command) {
        String key = paymentRequestValidator.validateIdempotencyKeyHeader(idempotencyKey);
        paymentIdempotencyService.requireNewPaymentLock(key, command);
        paymentRequestValidator.validateNewPayment(key, command);

        String transactionId = UUID.randomUUID().toString();
        duplicatePaymentChecker.reserveTransactionId(transactionId);

        return pendingPaymentService.savePendingPaymentAndPublishEvent(transactionId, key, command);
    }

    public PaymentResponse getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
