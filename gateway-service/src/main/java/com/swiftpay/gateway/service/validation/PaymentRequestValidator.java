package com.swiftpay.gateway.service.validation;

import com.swiftpay.gateway.cache.PaymentBalanceCache;
import com.swiftpay.gateway.client.LedgerBalanceClient;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.service.PaymentIdempotencyService;
import com.swiftpay.shared.exception.InsufficientFundsException;
import com.swiftpay.shared.exception.MissingIdempotencyKeyException;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PaymentRequestValidator {

    private final LedgerBalanceClient ledgerBalanceClient;
    private final PaymentBalanceCache paymentBalanceCache;
    private final PaymentIdempotencyService paymentIdempotencyService;

    public PaymentRequestValidator(
            LedgerBalanceClient ledgerBalanceClient,
            PaymentBalanceCache paymentBalanceCache,
            PaymentIdempotencyService paymentIdempotencyService) {
        this.ledgerBalanceClient = ledgerBalanceClient;
        this.paymentBalanceCache = paymentBalanceCache;
        this.paymentIdempotencyService = paymentIdempotencyService;
    }

    public String validateIdempotencyKeyHeader(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        return idempotencyKey.trim();
    }

    public void validateNewPayment(String idempotencyKey, PaymentCommand command) {
        try {
            rejectSelfTransfer(command);
            requireAccountsExist(command);
            refreshSenderBalance(command.senderId(), command.currency());
            requireSufficientBalance(command);
        } catch (IllegalArgumentException | InsufficientFundsException | UserNotFoundException ex) {
            paymentIdempotencyService.recordValidationFailure(idempotencyKey, command, ex.getMessage());
            throw ex;
        }
    }

    private static void rejectSelfTransfer(PaymentCommand command) {
        if (command.senderId().equals(command.receiverId())) {
            throw new IllegalArgumentException("senderId and receiverId must be different");
        }
    }

    private void requireAccountsExist(PaymentCommand command) {
        requireAccountExists(command.senderId(), command.currency());
        requireAccountExists(command.receiverId(), command.currency());
    }

    private void requireAccountExists(Long userId, String currency) {
        if (ledgerBalanceClient.getBalance(userId, currency).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
    }

    private void refreshSenderBalance(Long senderId, String currency) {
        ledgerBalanceClient.getBalance(senderId, currency)
                .ifPresent(balance -> paymentBalanceCache.setBalance(senderId, balance));
    }

    private void requireSufficientBalance(PaymentCommand command) {
        double available = paymentBalanceCache.getBalance(command.senderId(), command.currency())
                .orElseThrow(() -> new InsufficientFundsException(
                        command.senderId(),
                        command.currency(),
                        "no balance in Redis — refresh from ledger failed or account missing"));

        if (available < command.amount()) {
            throw new InsufficientFundsException(
                    command.senderId(), command.currency(), available, command.amount());
        }
    }
}
