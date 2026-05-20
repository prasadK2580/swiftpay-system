package com.swiftpay.gateway.infrastructure.persistence;

import com.swiftpay.gateway.application.mapper.PaymentTransactionMapper;
import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.TransactionWriter;
import com.swiftpay.gateway.repo.TransactionRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaTransactionWriter implements TransactionWriter {

    private final TransactionRepository transactionRepository;

    public JpaTransactionWriter(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    @Override
    public PaymentTransaction savePending(String transactionId, String idempotencyKey, PaymentCommand command) {
        PaymentTransaction transaction =
                PaymentTransactionMapper.toPendingEntity(transactionId, idempotencyKey, command);
        return transactionRepository.save(transaction);
    }
}
