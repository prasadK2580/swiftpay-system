package com.swiftpay.gateway.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.model.IdempotencyCheckResult;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.model.PaymentIdempotencyRedisEntry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Stores idempotency-key state in Redis (24h TTL).
 */
@Component
public class PaymentIdempotencyCache {

    static final String KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DuplicatePaymentChecker duplicatePaymentChecker;
    private final RedisTtlProperties redisTtlProperties;

    public PaymentIdempotencyCache(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            DuplicatePaymentChecker duplicatePaymentChecker,
            RedisTtlProperties redisTtlProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.duplicatePaymentChecker = duplicatePaymentChecker;
        this.redisTtlProperties = redisTtlProperties;
    }

    public IdempotencyCheckResult lockIdempotencyKeyOrReturnExisting(String idempotencyKey, PaymentCommand command) {
        String redisKey = redisKey(idempotencyKey);
        Object raw = redisTemplate.opsForValue().get(redisKey);
        if (raw == null) {
            return acquireLock(idempotencyKey, command);
        }
        return resolveExisting(idempotencyKey, command, toEntry(raw));
    }

    public void saveCompletedIdempotencyState(String idempotencyKey, String transactionId, PaymentCommand command) {
        PaymentIdempotencyRedisEntry entry = PaymentIdempotencyRedisEntry.completed(transactionId, command);
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), entry, ttl());
        duplicatePaymentChecker.markTransactionAsProcessed(transactionId);
    }

    public void deleteIdempotencyKey(String idempotencyKey) {
        redisTemplate.delete(redisKey(idempotencyKey));
    }

    public void saveFailedIdempotencyState(
            String idempotencyKey,
            String transactionId,
            PaymentCommand command,
            String errorMessage) {
        PaymentIdempotencyRedisEntry entry = PaymentIdempotencyRedisEntry.failed(transactionId, command, errorMessage);
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), entry, ttl());
        if (transactionId != null) {
            duplicatePaymentChecker.removeTransactionIdReservation(transactionId);
        }
    }

    private IdempotencyCheckResult acquireLock(String idempotencyKey, PaymentCommand command) {
        PaymentIdempotencyRedisEntry processing = PaymentIdempotencyRedisEntry.processing(command);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey(idempotencyKey), processing, ttl());

        if (Boolean.TRUE.equals(acquired)) {
            return IdempotencyCheckResult.proceed();
        }

        Object raw = redisTemplate.opsForValue().get(redisKey(idempotencyKey));
        if (raw == null) {
            return acquireLock(idempotencyKey, command);
        }
        return resolveExisting(idempotencyKey, command, toEntry(raw));
    }

    private IdempotencyCheckResult resolveExisting(
            String idempotencyKey,
            PaymentCommand command,
            PaymentIdempotencyRedisEntry existing) {

        if (!existing.matchesCommand(command)) {
            return IdempotencyCheckResult.conflict();
        }

        if (existing.getState() == PaymentIdempotencyRedisEntry.State.COMPLETED
                && existing.getTransactionId() != null) {
            if (duplicatePaymentChecker.isTransactionAlreadyProcessed(existing.getTransactionId())) {
                return IdempotencyCheckResult.alreadyProcessed(existing.getTransactionId());
            }
            return IdempotencyCheckResult.alreadyProcessed(existing.getTransactionId());
        }

        if (existing.getState() == PaymentIdempotencyRedisEntry.State.FAILED) {
            redisTemplate.delete(redisKey(idempotencyKey));
            return acquireLock(idempotencyKey, command);
        }

        return IdempotencyCheckResult.inProgress(existing.getTransactionId());
    }

    private PaymentIdempotencyRedisEntry toEntry(Object raw) {
        if (raw instanceof PaymentIdempotencyRedisEntry entry) {
            return entry;
        }
        return objectMapper.convertValue(raw, PaymentIdempotencyRedisEntry.class);
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }

    static String redisKey(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }
}
