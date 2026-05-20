/**
 * Service A (Transaction Gateway): accepts payments, idempotency, balance checks,
 * saves PENDING transactions, publishes Kafka events, applies settlement feedback.
 */
package com.swiftpay.gateway;
