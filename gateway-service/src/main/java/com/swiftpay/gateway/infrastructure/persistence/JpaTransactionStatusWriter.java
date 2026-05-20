package com.swiftpay.gateway.infrastructure.persistence;

import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.port.TransactionStatusWriter;
import com.swiftpay.gateway.repo.TransactionRepository;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JpaTransactionStatusWriter implements TransactionStatusWriter {

    private static final Logger log = LoggerFactory.getLogger(JpaTransactionStatusWriter.class);

    private final TransactionRepository transactionRepository;

    public JpaTransactionStatusWriter(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    @Override
    public boolean updateStatusIfPending(String transactionId, TransactionStatus newStatus) {
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        if (transaction.getStatus() == newStatus) {
            return false;
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Ignoring status update transactionId={} current={} target={}",
                    transactionId, transaction.getStatus(), newStatus);
            return false;
        }

        transaction.setStatus(newStatus);
        transactionRepository.save(transaction);
        return true;
    }
}
