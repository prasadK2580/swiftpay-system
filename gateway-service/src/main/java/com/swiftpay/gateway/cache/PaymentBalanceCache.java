package com.swiftpay.gateway.cache;

import com.swiftpay.gateway.config.RedisTtlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Cached sender balances in Redis (refreshed from the ledger before validation).
 */
@Component
public class PaymentBalanceCache {

    private static final Logger log = LoggerFactory.getLogger(PaymentBalanceCache.class);
    static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTtlProperties redisTtlProperties;

    public PaymentBalanceCache(StringRedisTemplate stringRedisTemplate, RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTtlProperties = redisTtlProperties;
    }

    public Optional<Double> getBalance(Long senderId, String currency) {
        String raw = stringRedisTemplate.opsForValue().get(redisKey(senderId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException ex) {
            log.error("invalid balance cache userId={}", senderId, ex);
            return Optional.empty();
        }
    }

    public void setBalance(Long senderId, double balance) {
        stringRedisTemplate.opsForValue().set(redisKey(senderId), String.valueOf(balance), ttl());
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }

    static String redisKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
