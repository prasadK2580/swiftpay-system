package com.swiftpay.ledger.config;

import com.swiftpay.shared.config.KafkaTopicAdminProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final KafkaTopicAdminProperties topicAdmin;

    public KafkaTopicConfig(KafkaTopicAdminProperties topicAdmin) {
        this.topicAdmin = topicAdmin;
    }

    @Bean
    public NewTopic paymentInitiatedTopic(
            @Value("${app.kafka.topics.payment-initiated}") String name) {
        return newTopic(name);
    }

    @Bean
    public NewTopic paymentCompletedTopic(
            @Value("${app.kafka.topics.payment-completed}") String name) {
        return newTopic(name);
    }

    @Bean
    public NewTopic paymentFailedTopic(
            @Value("${app.kafka.topics.payment-failed}") String name) {
        return newTopic(name);
    }

    private NewTopic newTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(topicAdmin.getPartitions())
                .replicas(topicAdmin.getReplicas())
                .build();
    }
}
