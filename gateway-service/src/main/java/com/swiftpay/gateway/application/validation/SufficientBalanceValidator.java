package com.swiftpay.gateway.application.validation;

import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.port.BalanceStore;
import com.swiftpay.shared.exception.InsufficientFundsException;
import org.springframework.stereotype.Component;

@Component
public class SufficientBalanceValidator implements PaymentValidator {

    private final BalanceStore balanceStore;

    public SufficientBalanceValidator(BalanceStore balanceStore) {
        this.balanceStore = balanceStore;
    }


    @Override
    public void validate(PaymentCommand command) {
        double available = balanceStore.getBalance(command.senderId(), command.currency())
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
