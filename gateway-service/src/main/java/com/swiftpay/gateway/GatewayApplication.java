package com.swiftpay.gateway;

import com.swiftpay.gateway.config.KafkaConsumerRetryProperties;
import com.swiftpay.gateway.config.LedgerHttpProperties;
import com.swiftpay.gateway.config.RedisTtlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.swiftpay.gateway", "com.swiftpay.shared"})
@EntityScan(basePackages = "com.swiftpay.gateway.entity")
@EnableJpaRepositories(basePackages = "com.swiftpay.gateway.repo")
@EnableConfigurationProperties({
        RedisTtlProperties.class,
        KafkaConsumerRetryProperties.class,
        LedgerHttpProperties.class
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
