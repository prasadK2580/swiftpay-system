package com.swiftpay.gateway.port;

import com.swiftpay.shared.domain.enums.TransactionStatus;

/**
 * Updates transaction status after settlement feedback (ISP — separate from create/read).
 */
public interface TransactionStatusWriter {

    /**
     * @return true if status was updated from PENDING to {@code newStatus}
     */
    boolean updateStatusIfPending(String transactionId, TransactionStatus newStatus);
}
