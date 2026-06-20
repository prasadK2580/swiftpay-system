package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.controller.dto.LedgerBalanceResponse;
import com.swiftpay.ledger.service.LedgerService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/v1/accounts")
public class LedgerBalanceController {

    private final LedgerService ledgerService;

    public LedgerBalanceController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }


    @GetMapping("/{userId}/balance")
    public LedgerBalanceResponse getBalance(
            @PathVariable @Positive Long userId,
            @RequestParam @NotBlank String currency) {
        return ledgerService.getBalance(userId, currency);
    }
}
