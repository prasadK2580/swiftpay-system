package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.GatewayApplication;
import com.swiftpay.ledger.LedgerApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Starts ledger + gateway in-process for cross-service integration tests (shared Postgres, Redis, Kafka).
 */
@SpringBootTest(classes = GatewayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Sql(scripts = "/sql/integration-test-reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class IntegrationTestBase {

    private static final String GROUP_SUFFIX = UUID.randomUUID().toString();
    private static ConfigurableApplicationContext ledgerContext;
    private static int ledgerPort;

    @DynamicPropertySource
    static void integrationProperties(DynamicPropertyRegistry registry) {
        ensureLedgerStarted();
        registry.add("app.ledger.http.base-url", () -> "http://localhost:" + ledgerPort);
        registry.add("app.gateway.consumer.group-id", () -> "swiftpay-it-gateway-" + GROUP_SUFFIX);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "latest");
    }

    @BeforeAll
    static void startLedger() {
        ensureLedgerStarted();
    }

    @AfterAll
    static void stopLedger() {
        if (ledgerContext != null) {
            ledgerContext.close();
            ledgerContext = null;
        }
    }

    private static void ensureLedgerStarted() {
        if (ledgerContext != null) {
            return;
        }
        synchronized (IntegrationTestBase.class) {
            if (ledgerContext != null) {
                return;
            }
            try {
                ledgerPort = findFreePort();
                String ledgerGroup = "swiftpay-it-ledger-" + GROUP_SUFFIX;
                ledgerContext = new SpringApplicationBuilder(LedgerApplication.class)
                        .profiles("integration-test")
                        .properties("spring.main.banner-mode=off")
                        .run(
                                "--server.port=" + ledgerPort,
                                "--app.ledger.consumer.group-id=" + ledgerGroup,
                                "--spring.kafka.consumer.auto-offset-reset=latest");

                await().atMost(30, SECONDS).ignoreExceptions().until(() -> {
                    var conn = java.net.URI.create("http://localhost:" + ledgerPort + "/health")
                            .toURL()
                            .openConnection();
                    conn.setConnectTimeout(2000);
                    conn.connect();
                    return true;
                });
                Thread.sleep(5000);
            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Failed to start ledger for integration tests", ex);
            }
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
