package com.swiftpay.gateway.infrastructure.redis;

import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.port.TransactionDeduplicationGuard;
import com.swiftpay.gateway.port.TransactionReader;
import com.swiftpay.shared.exception.IdempotencyConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTransactionDeduplicationGuard implements TransactionDeduplicationGuard {

    static final String KEY_PREFIX = "tx:processed:";
    static final String STATE_RESERVED = "RESERVED";
    static final String STATE_COMPLETED = "COMPLETED";

    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionReader transactionReader;
    private final RedisTtlProperties redisTtlProperties;

    public RedisTransactionDeduplicationGuard(StringRedisTemplate stringRedisTemplate, TransactionReader transactionReader, RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.transactionReader = transactionReader;
        this.redisTtlProperties = redisTtlProperties;
    }


    @Override
    public void reserveBeforePersist(String transactionId) {
        if (isAlreadyProcessed(transactionId)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }
        if (transactionReader.existsByTransactionId(transactionId)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey(transactionId), STATE_RESERVED, ttl());
        if (!Boolean.TRUE.equals(acquired)) {
            throw IdempotencyConflictException.transactionAlreadyProcessed(transactionId);
        }
    }

    @Override
    public void confirmProcessed(String transactionId) {
        stringRedisTemplate.opsForValue().set(redisKey(transactionId), STATE_COMPLETED, ttl());
    }

    @Override
    public void releaseReservation(String transactionId) {
        String key = redisKey(transactionId);
        if (STATE_RESERVED.equals(stringRedisTemplate.opsForValue().get(key))) {
            stringRedisTemplate.delete(key);
        }
    }

    @Override
    public boolean isAlreadyProcessed(String transactionId) {
        return STATE_COMPLETED.equals(stringRedisTemplate.opsForValue().get(redisKey(transactionId)));
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }

    static String redisKey(String transactionId) {
        return KEY_PREFIX + transactionId;
    }
}
