package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.GatewayApplication;
import com.swiftpay.ledger.LedgerApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

                await().atMost(60, SECONDS)
                        .pollInterval(500, MILLISECONDS)
                        .ignoreExceptions()
                        .until(IntegrationTestBase::seedAccountBalanceReady);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to start ledger for integration tests", ex);
            }
        }
    }

    private static boolean seedAccountBalanceReady() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(
                        "http://localhost:" + ledgerPort + "/v1/accounts/1001/balance?currency=INR")
                .toURL()
                .openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
