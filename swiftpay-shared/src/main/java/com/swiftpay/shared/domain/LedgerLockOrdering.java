package com.swiftpay.shared.domain;

/**
 * Consistent ordering for ledger resources (deadlock prevention + Kafka partition affinity).
 */
public final class LedgerLockOrdering {

    private LedgerLockOrdering() {
    }

    public static long firstLockUserId(long userIdA, long userIdB) {
        return Math.min(userIdA, userIdB);
    }

    public static long secondLockUserId(long userIdA, long userIdB) {
        return Math.max(userIdA, userIdB);
    }

    /** Kafka key so related payments for the same account pair land on one partition when possible. */
    public static String kafkaPartitionKey(long senderId, long receiverId) {
        return firstLockUserId(senderId, receiverId) + ":" + secondLockUserId(senderId, receiverId);
    }
}
