package com.swiftpay.gateway.application.mapper;

import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

public final class PaymentTransactionMapper {

    private PaymentTransactionMapper() {
    }

    public static PaymentTransaction toPendingEntity(
            String transactionId, String idempotencyKey, PaymentCommand command) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setSenderId(command.senderId());
        transaction.setReceiverId(command.receiverId());
        transaction.setAmount(command.amount());
        transaction.setCurrency(command.currency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
}
