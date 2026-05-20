package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.controller.dto.TransactionHistoryItem;
import com.swiftpay.ledger.service.TransactionHistoryUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/v1/history")
public class LedgerHistoryController {

    private final TransactionHistoryUseCase transactionHistoryUseCase;

    public LedgerHistoryController(TransactionHistoryUseCase transactionHistoryUseCase) {
        this.transactionHistoryUseCase = transactionHistoryUseCase;
    }


    @GetMapping("/{userId}")
    public List<TransactionHistoryItem> getHistory(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return transactionHistoryUseCase.getHistoryForUser(userId, limit);
    }
}
