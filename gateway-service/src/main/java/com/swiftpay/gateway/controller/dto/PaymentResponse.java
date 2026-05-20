package com.swiftpay.gateway.controller.dto;

import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

public class PaymentResponse {

    private String transactionId;
    private String idempotencyKey;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    public static PaymentResponse from(PaymentTransaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setIdempotencyKey(transaction.getIdempotencyKey());
        response.setSenderId(transaction.getSenderId());
        response.setReceiverId(transaction.getReceiverId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
