package com.swiftpay.ledger.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.net.SocketException;
import java.sql.SQLTransientException;

@Configuration
public class KafkaConsumerRetryConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerRetryConfig.class);

    @Bean
    public DefaultErrorHandler kafkaConsumerErrorHandler(KafkaConsumerRetryProperties properties) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(properties.getInitialIntervalMs());
        backOff.setMultiplier(properties.getMultiplier());
        backOff.setMaxInterval(properties.getMaxIntervalMs());
        backOff.setMaxElapsedTime(computeMaxElapsedTime(properties));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                (ConsumerRecord<?, ?> record, Exception ex) -> log.error(
                        "Kafka consumer retries exhausted topic={} partition={} offset={}",
                        record.topic(), record.partition(), record.offset(), ex),
                backOff);

        handler.addRetryableExceptions(
                TransientDataAccessException.class,
                DataAccessResourceFailureException.class,
                CannotGetJdbcConnectionException.class,
                CannotCreateTransactionException.class,
                PessimisticLockingFailureException.class,
                SQLTransientException.class,
                JDBCConnectionException.class,
                SocketException.class);

        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class);

        return handler;
    }

    static long computeMaxElapsedTime(KafkaConsumerRetryProperties properties) {
        long total = 0;
        long interval = properties.getInitialIntervalMs();
        for (int attempt = 0; attempt < properties.getMaxAttempts(); attempt++) {
            total += interval;
            interval = (long) Math.min(interval * properties.getMultiplier(), properties.getMaxIntervalMs());
        }
        return total;
    }
}
