package com.swiftpay.gateway.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E: POST payment → poll GET /v1/payments/{id} until COMPLETED.
 */
class PaymentStatusIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPaymentStatus_afterSettlement_returnsCompleted() throws Exception {
        String body = """
                {"senderId":1001,"receiverId":2002,"amount":1,"currency":"INR"}
                """;
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult createResult = mockMvc.perform(post("/v1/payments")
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        String transactionId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.transactionId");

        await().atMost(30, SECONDS).pollInterval(1, SECONDS).untilAsserted(() ->
                mockMvc.perform(get("/v1/payments/{transactionId}", transactionId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status", is("COMPLETED"))));
    }

    @Test
    void getPaymentStatus_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/v1/payments/{transactionId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
