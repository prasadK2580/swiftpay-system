package com.swiftpay.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gateway rejects payments when receiver account does not exist in the ledger (404).
 */
class UnknownReceiverIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPayment_returns404_whenReceiverAccountMissing() throws Exception {
        String body = """
                {"senderId":1001,"receiverId":3003,"amount":1,"currency":"INR"}
                """;

        mockMvc.perform(post("/v1/payments")
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
