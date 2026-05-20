package com.swiftpay.gateway.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.gateway.application.idempotency.IdempotencyOutcome;
import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.model.IdempotencyCacheEntry;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.IdempotencyGuard;
import com.swiftpay.gateway.port.TransactionDeduplicationGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyGuard implements IdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyGuard.class);
    static final String KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionDeduplicationGuard transactionDeduplicationGuard;
    private final RedisTtlProperties redisTtlProperties;

    public RedisIdempotencyGuard(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, TransactionDeduplicationGuard transactionDeduplicationGuard, RedisTtlProperties redisTtlProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.transactionDeduplicationGuard = transactionDeduplicationGuard;
        this.redisTtlProperties = redisTtlProperties;
    }


    @Override
    public IdempotencyOutcome checkAndLock(String idempotencyKey, PaymentCommand command) {
        String redisKey = redisKey(idempotencyKey);
        Object raw = redisTemplate.opsForValue().get(redisKey);
        if (raw == null) {
            return acquireLock(idempotencyKey, command);
        }
        return resolveExisting(idempotencyKey, command, toEntry(raw));
    }

    @Override
    public void markCompleted(String idempotencyKey, String transactionId, PaymentCommand command) {
        IdempotencyCacheEntry entry = IdempotencyCacheEntry.completed(transactionId, command);
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), entry, ttl());
        transactionDeduplicationGuard.confirmProcessed(transactionId);
    }

    @Override
    public void releaseLock(String idempotencyKey) {
        redisTemplate.delete(redisKey(idempotencyKey));
    }

    @Override
    public void markFailed(
            String idempotencyKey,
            String transactionId,
            PaymentCommand command,
            String errorMessage) {
        IdempotencyCacheEntry entry = IdempotencyCacheEntry.failed(transactionId, command, errorMessage);
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), entry, ttl());
        if (transactionId != null) {
            transactionDeduplicationGuard.releaseReservation(transactionId);
        }
        log.warn("Idempotency failed key={} error={}", idempotencyKey, errorMessage);
    }

    private IdempotencyOutcome acquireLock(String idempotencyKey, PaymentCommand command) {
        IdempotencyCacheEntry processing = IdempotencyCacheEntry.processing(command);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey(idempotencyKey), processing, ttl());

        if (Boolean.TRUE.equals(acquired)) {
            return IdempotencyOutcome.proceed();
        }

        Object raw = redisTemplate.opsForValue().get(redisKey(idempotencyKey));
        if (raw == null) {
            return acquireLock(idempotencyKey, command);
        }
        return resolveExisting(idempotencyKey, command, toEntry(raw));
    }

    private IdempotencyOutcome resolveExisting(
            String idempotencyKey,
            PaymentCommand command,
            IdempotencyCacheEntry existing) {

        if (!existing.matchesCommand(command)) {
            log.warn("Idempotency conflict key={}", idempotencyKey);
            return IdempotencyOutcome.conflict();
        }

        if (existing.getState() == IdempotencyCacheEntry.State.COMPLETED
                && existing.getTransactionId() != null) {
            if (transactionDeduplicationGuard.isAlreadyProcessed(existing.getTransactionId())) {
                return IdempotencyOutcome.alreadyProcessed(existing.getTransactionId());
            }
            log.warn("Idempotency completed but tx missing key={} transactionId={}",
                    idempotencyKey, existing.getTransactionId());
            return IdempotencyOutcome.alreadyProcessed(existing.getTransactionId());
        }

        if (existing.getState() == IdempotencyCacheEntry.State.FAILED) {
            redisTemplate.delete(redisKey(idempotencyKey));
            return acquireLock(idempotencyKey, command);
        }

        return IdempotencyOutcome.inProgress(existing.getTransactionId());
    }

    private IdempotencyCacheEntry toEntry(Object raw) {
        if (raw instanceof IdempotencyCacheEntry entry) {
            return entry;
        }
        return objectMapper.convertValue(raw, IdempotencyCacheEntry.class);
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }

    static String redisKey(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }
}
