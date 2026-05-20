package com.swiftpay.gateway.port;

import com.swiftpay.shared.domain.enums.TransactionStatus;

/**
 * Redis cache for final transaction status (ISP — separate from balance cache).
 */
public interface TransactionStatusCache {

    void writeStatus(String transactionId, TransactionStatus status);
}
