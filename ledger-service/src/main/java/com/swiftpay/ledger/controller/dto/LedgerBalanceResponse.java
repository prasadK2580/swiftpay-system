package com.swiftpay.ledger.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authoritative account balance from the ledger (Service B)")
public record LedgerBalanceResponse(
        @Schema(example = "1001") Long userId,
        @Schema(example = "10000.0") double balance,
        @Schema(example = "INR") String currency) {
}
