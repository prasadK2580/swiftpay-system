package com.swiftpay.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requires Postgres, Redis, and Kafka on localhost (see docker compose for CI).
 */
class PaymentApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPayment_returns201() throws Exception {
        String body = """
                {"senderId":1001,"receiverId":2002,"amount":1,"currency":"INR"}
                """;

        mockMvc.perform(post("/v1/payments")
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
