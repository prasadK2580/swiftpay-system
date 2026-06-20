package com.swiftpay.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka topic creation defaults (used by gateway and ledger {@code KafkaTopicConfig}).
 */
@ConfigurationProperties(prefix = "app.kafka.admin")
public class KafkaTopicAdminProperties {

    private int partitions = 6;
    private int replicas = 1;

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }
}
