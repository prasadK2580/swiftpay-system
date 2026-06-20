package com.swiftpay.gateway.cache;

import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis cache for final transaction status after settlement feedback.
 */
@Component
public class PaymentStatusCache {

    static final String KEY_PREFIX = "tx:status:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTtlProperties redisTtlProperties;

    public PaymentStatusCache(StringRedisTemplate stringRedisTemplate, RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTtlProperties = redisTtlProperties;
    }

    public void writeStatus(String transactionId, TransactionStatus status) {
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + transactionId,
                status.name(),
                ttl());
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }
}
