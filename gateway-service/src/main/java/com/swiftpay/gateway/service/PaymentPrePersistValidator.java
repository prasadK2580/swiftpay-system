package com.swiftpay.gateway.service;

import com.swiftpay.gateway.application.validation.AccountExistenceValidator;
import com.swiftpay.gateway.application.validation.PaymentBusinessRulesValidator;
import com.swiftpay.gateway.application.validation.SufficientBalanceValidator;
import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.shared.exception.InsufficientFundsException;
import com.swiftpay.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class PaymentPrePersistValidator {

    private final PaymentBusinessRulesValidator businessRulesValidator;
    private final AccountExistenceValidator accountExistenceValidator;
    private final BalanceCacheRefresher balanceCacheRefresher;
    private final SufficientBalanceValidator sufficientBalanceValidator;
    private final IdempotencyCoordinator idempotencyCoordinator;

    public PaymentPrePersistValidator(PaymentBusinessRulesValidator businessRulesValidator, AccountExistenceValidator accountExistenceValidator, BalanceCacheRefresher balanceCacheRefresher, SufficientBalanceValidator sufficientBalanceValidator, IdempotencyCoordinator idempotencyCoordinator) {
        this.businessRulesValidator = businessRulesValidator;
        this.accountExistenceValidator = accountExistenceValidator;
        this.balanceCacheRefresher = balanceCacheRefresher;
        this.sufficientBalanceValidator = sufficientBalanceValidator;
        this.idempotencyCoordinator = idempotencyCoordinator;
    }


    public void validate(String idempotencyKey, PaymentCommand command) {
        try {
            businessRulesValidator.validate(command);
            accountExistenceValidator.validate(command);
            balanceCacheRefresher.refreshSenderBalanceFromLedger(command.senderId(), command.currency());
            sufficientBalanceValidator.validate(command);
        } catch (IllegalArgumentException | InsufficientFundsException | UserNotFoundException ex) {
            idempotencyCoordinator.markValidationFailed(idempotencyKey, command, ex.getMessage());
            throw ex;
        }
    }
}
