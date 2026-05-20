package com.swiftpay.ledger.model;

/**
 * Ledger view of an account (decoupled from gateway JPA entity).
 */
public record SettlementAccount(Long userId, double balance, String currency) {
}
