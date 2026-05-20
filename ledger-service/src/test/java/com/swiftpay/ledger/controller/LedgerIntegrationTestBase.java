package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.LedgerApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

@SpringBootTest(classes = LedgerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Sql(scripts = "/sql/integration-test-reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class LedgerIntegrationTestBase {

    private static final String GROUP_SUFFIX = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void integrationKafkaGroups(DynamicPropertyRegistry registry) {
        registry.add("app.ledger.consumer.group-id", () -> "swiftpay-it-ledger-" + GROUP_SUFFIX);
    }
}
