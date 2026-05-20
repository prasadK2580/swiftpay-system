package com.swiftpay.gateway.model;

/**
 * Application input for creating a payment (DIP — ports and services do not depend on HTTP DTOs).
 */
public record PaymentCommand(Long senderId, Long receiverId, Double amount, String currency) {
}
