package com.swiftpay.gateway.application.validation;

import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.LedgerBalanceReader;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AccountExistenceValidator implements PaymentValidator {

    private final LedgerBalanceReader ledgerBalanceReader;

    public AccountExistenceValidator(LedgerBalanceReader ledgerBalanceReader) {
        this.ledgerBalanceReader = ledgerBalanceReader;
    }


    @Override
    public void validate(PaymentCommand command) {
        ensureAccountExists(command.senderId(), command.currency());
        ensureAccountExists(command.receiverId(), command.currency());
    }

    private void ensureAccountExists(Long userId, String currency) {
        if (ledgerBalanceReader.getBalance(userId, currency).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
    }
}
