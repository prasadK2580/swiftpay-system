package com.swiftpay.ledger;

import com.swiftpay.ledger.config.KafkaConsumerRetryProperties;
import com.swiftpay.ledger.config.RedisTtlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.swiftpay.ledger", "com.swiftpay.shared"})
@EntityScan(basePackages = "com.swiftpay.ledger.entity")
@EnableJpaRepositories(basePackages = "com.swiftpay.ledger.repo")
@EnableConfigurationProperties({
        RedisTtlProperties.class,
        KafkaConsumerRetryProperties.class
})
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
