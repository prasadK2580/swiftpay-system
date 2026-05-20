package com.swiftpay.gateway.port;

import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.model.PaymentCommand;

/**
 * Port for persisting transactions (DIP, ISP — separate from reads).
 */
public interface TransactionWriter {

    PaymentTransaction savePending(String transactionId, String idempotencyKey, PaymentCommand command);
}
