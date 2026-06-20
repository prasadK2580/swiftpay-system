package com.swiftpay.gateway.cache;

import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.repository.PaymentRepository;
import com.swiftpay.shared.exception.IdempotencyConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis guard so the same {@code transactionId} is not used twice within the TTL window.
 */
@Component
public class DuplicatePaymentChecker {

    static final String KEY_PREFIX = "tx:processed:";
    static final String STATE_RESERVED = "RESERVED";
    static final String STATE_COMPLETED = "COMPLETED";

    private final StringRedisTemplate stringRedisTemplate;
    private final PaymentRepository paymentRepository;
    private final RedisTtlProperties redisTtlProperties;

    public DuplicatePaymentChecker(
            StringRedisTemplate stringRedisTemplate,
            PaymentRepository paymentRepository,
            RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.paymentRepository = paymentRepository;
        this.redisTtlProperties = redisTtlProperties;
    }

    public void reserveTransactionId(String transactionId) {
        if (isTransactionAlreadyProcessed(transactionId)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }
        if (paymentRepository.existsByTransactionId(transactionId)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey(transactionId), STATE_RESERVED, ttl());
        if (!Boolean.TRUE.equals(acquired)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }
    }

    public void markTransactionAsProcessed(String transactionId) {
        stringRedisTemplate.opsForValue().set(redisKey(transactionId), STATE_COMPLETED, ttl());
    }

    public void removeTransactionIdReservation(String transactionId) {
        String key = redisKey(transactionId);
        if (STATE_RESERVED.equals(stringRedisTemplate.opsForValue().get(key))) {
            stringRedisTemplate.delete(key);
        }
    }

    public boolean isTransactionAlreadyProcessed(String transactionId) {
        return STATE_COMPLETED.equals(stringRedisTemplate.opsForValue().get(redisKey(transactionId)));
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }

    static String redisKey(String transactionId) {
        return KEY_PREFIX + transactionId;
    }
}
