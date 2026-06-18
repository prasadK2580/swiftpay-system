package com.swiftpay.gateway.model;

/**
 * Redis value (24h TTL). Key: {@code idempotency:{Idempotency-Key header}}.
 */
public class PaymentIdempotencyRedisEntry {

    public enum State {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    private State state;
    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String currency;
    private String errorMessage;

    public PaymentIdempotencyRedisEntry() {
    }

    public static PaymentIdempotencyRedisEntry processing(PaymentCommand command) {
        PaymentIdempotencyRedisEntry entry = new PaymentIdempotencyRedisEntry();
        entry.setState(State.PROCESSING);
        entry.setSenderId(command.senderId());
        entry.setReceiverId(command.receiverId());
        entry.setAmount(command.amount());
        entry.setCurrency(command.currency());
        return entry;
    }

    public static PaymentIdempotencyRedisEntry completed(String transactionId, PaymentCommand command) {
        PaymentIdempotencyRedisEntry entry = processing(command);
        entry.setState(State.COMPLETED);
        entry.setTransactionId(transactionId);
        entry.setErrorMessage(null);
        return entry;
    }

    public static PaymentIdempotencyRedisEntry failed(
            String transactionId,
            PaymentCommand command,
            String errorMessage) {
        PaymentIdempotencyRedisEntry entry = processing(command);
        entry.setState(State.FAILED);
        entry.setTransactionId(transactionId);
        entry.setErrorMessage(errorMessage);
        return entry;
    }

    public boolean matchesCommand(PaymentCommand command) {
        return senderId.equals(command.senderId())
                && receiverId.equals(command.receiverId())
                && amount.equals(command.amount())
                && currency.equals(command.currency());
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
