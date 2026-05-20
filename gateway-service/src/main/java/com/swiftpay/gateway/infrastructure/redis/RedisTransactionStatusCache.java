package com.swiftpay.gateway.infrastructure.redis;

import com.swiftpay.gateway.config.RedisTtlProperties;
import com.swiftpay.gateway.port.TransactionStatusCache;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTransactionStatusCache implements TransactionStatusCache {

    private static final Logger log = LoggerFactory.getLogger(RedisTransactionStatusCache.class);
    static final String KEY_PREFIX = "tx:status:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTtlProperties redisTtlProperties;

    public RedisTransactionStatusCache(StringRedisTemplate stringRedisTemplate, RedisTtlProperties redisTtlProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTtlProperties = redisTtlProperties;
    }


    @Override
    public void writeStatus(String transactionId, TransactionStatus status) {
        try {
            stringRedisTemplate.opsForValue().set(
                    KEY_PREFIX + transactionId,
                    status.name(),
                    ttl());
        } catch (Exception ex) {
            log.warn("Failed to cache tx status transactionId={}", transactionId, ex);
        }
    }

    private Duration ttl() {
        return Duration.ofHours(redisTtlProperties.getTtlHours());
    }
}
