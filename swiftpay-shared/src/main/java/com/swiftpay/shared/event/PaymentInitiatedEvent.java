package com.swiftpay.shared.event;

import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

/**
 * Emitted by Service A (Transaction Gateway) to Kafka after a payment is persisted as PENDING.
 * Signals the ecosystem (e.g. Service B / Ledger) that the transaction is ready for settlement processing.
 */
public class PaymentInitiatedEvent {

    public static final String EVENT_TYPE = "PaymentInitiated";

    private String eventType;
    private String idempotencyKey;
    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime occurredAt;

    public PaymentInitiatedEvent() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
