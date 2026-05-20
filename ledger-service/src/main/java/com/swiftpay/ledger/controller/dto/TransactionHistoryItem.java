package com.swiftpay.ledger.controller.dto;
import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

public class TransactionHistoryItem {

    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime timestamp;

    public static TransactionHistoryItem from(PaymentTransaction transaction) {
        TransactionHistoryItem item = new TransactionHistoryItem();
        item.setTransactionId(transaction.getTransactionId());
        item.setSenderId(transaction.getSenderId());
        item.setReceiverId(transaction.getReceiverId());
        item.setAmount(transaction.getAmount());
        item.setCurrency(transaction.getCurrency());
        item.setStatus(transaction.getStatus());
        item.setTimestamp(transaction.getCreatedAt());
        return item;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
