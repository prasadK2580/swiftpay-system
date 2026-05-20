package com.swiftpay.ledger.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerBalanceApiIntegrationTest extends LedgerIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getBalance_returns200_forSeedAccount() throws Exception {
        mockMvc.perform(get("/v1/accounts/{userId}/balance", 1001).param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.balance").isNumber());
    }

    @Test
    void getBalance_returns404_forUnknownUser() throws Exception {
        mockMvc.perform(get("/v1/accounts/{userId}/balance", 99999).param("currency", "INR"))
                .andExpect(status().isNotFound());
    }
}
