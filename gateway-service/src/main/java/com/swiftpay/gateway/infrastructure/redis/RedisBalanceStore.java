package com.swiftpay.gateway.infrastructure.redis;

import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.port.BalanceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisBalanceStore implements BalanceStore {

    private static final Logger log = LoggerFactory.getLogger(RedisBalanceStore.class);
    static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTtlProperties redisTtlProperties;

    public RedisBalanceStore(StringRedisTemplate stringRedisTemplate, RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTtlProperties = redisTtlProperties;
    }


    @Override
    public Optional<Double> getBalance(Long senderId, String currency) {
        String raw = stringRedisTemplate.opsForValue().get(redisKey(senderId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException ex) {
            log.error("Invalid balance in Redis userId={} value={}", senderId, raw, ex);
            return Optional.empty();
        }
    }

    @Override
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
