package com.swiftpay.ledger.infrastructure.redis;

import com.swiftpay.ledger.config.RedisTtlProperties;
import com.swiftpay.ledger.port.BalanceStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisBalanceStore implements BalanceStore {

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
