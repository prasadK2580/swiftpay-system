package com.swiftpay.shared.event;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

public class PaymentCompletedEvent {

    public static final String EVENT_TYPE = "PaymentCompleted";

    private String eventType;
    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime occurredAt;

    public static PaymentCompletedEvent from(PaymentInitiatedEvent initiated) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setEventType(EVENT_TYPE);
        event.setTransactionId(initiated.getTransactionId());
        event.setSenderId(initiated.getSenderId());
        event.setReceiverId(initiated.getReceiverId());
        event.setAmount(initiated.getAmount());
        event.setCurrency(initiated.getCurrency());
        event.setStatus(TransactionStatus.COMPLETED);
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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
