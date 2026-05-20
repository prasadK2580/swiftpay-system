package com.swiftpay.ledger.service;

import com.swiftpay.ledger.model.SettlementAccount;
import com.swiftpay.ledger.model.SettlementResult;
import com.swiftpay.ledger.model.SettlementTransaction;
import com.swiftpay.ledger.port.SettlementAccountStore;
import com.swiftpay.ledger.port.SettlementTransactionStore;
import com.swiftpay.shared.domain.LedgerLockOrdering;
import com.swiftpay.shared.domain.SettlementFailureReason;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import com.swiftpay.shared.event.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerSettlementService {

    private static final Logger log = LoggerFactory.getLogger(LedgerSettlementService.class);

    private final PaymentInitiatedEventValidator eventValidator;
    private final SettlementTransactionStore transactionStore;
    private final SettlementAccountStore accountStore;

    public LedgerSettlementService(PaymentInitiatedEventValidator eventValidator, SettlementTransactionStore transactionStore, SettlementAccountStore accountStore) {
        this.eventValidator = eventValidator;
        this.transactionStore = transactionStore;
        this.accountStore = accountStore;
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SettlementResult settlePayment(PaymentInitiatedEvent event) {
        eventValidator.validate(event);

        SettlementTransaction transaction = transactionStore
                .findByTransactionIdForUpdate(event.getTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction not found: " + event.getTransactionId()));

        if (transaction.status() != TransactionStatus.PENDING) {
            return SettlementResult.skipped();
        }

        Long senderId = event.getSenderId();
        Long receiverId = event.getReceiverId();
        double amount = event.getAmount();
        String currency = event.getCurrency();

        Long firstUserId = LedgerLockOrdering.firstLockUserId(senderId, receiverId);
        Long secondUserId = LedgerLockOrdering.secondLockUserId(senderId, receiverId);

        var firstOpt = accountStore.findByIdForUpdate(firstUserId);
        var secondOpt = accountStore.findByIdForUpdate(secondUserId);
        if (firstOpt.isEmpty() || secondOpt.isEmpty()) {
            return failAtomically(event.getTransactionId(), SettlementFailureReason.ACCOUNT_NOT_FOUND);
        }

        SettlementAccount first = firstOpt.get();
        SettlementAccount second = secondOpt.get();
        SettlementAccount sender = senderId.equals(first.userId()) ? first : second;
        SettlementAccount receiver = receiverId.equals(first.userId()) ? first : second;

        if (!sender.currency().equalsIgnoreCase(currency)
                || !receiver.currency().equalsIgnoreCase(currency)) {
            return failAtomically(event.getTransactionId(), sender, SettlementFailureReason.CURRENCY_MISMATCH);
        }

        if (sender.balance() < amount) {
            return failAtomically(event.getTransactionId(), sender, SettlementFailureReason.INSUFFICIENT_FUNDS);
        }

        SettlementAccount debitedSender = new SettlementAccount(
                sender.userId(), sender.balance() - amount, sender.currency());
        SettlementAccount creditedReceiver = new SettlementAccount(
                receiver.userId(), receiver.balance() + amount, receiver.currency());
        accountStore.save(debitedSender);
        accountStore.save(creditedReceiver);
        transactionStore.updateStatus(event.getTransactionId(), TransactionStatus.COMPLETED);

        log.info("Settled transactionId={} amount={}", event.getTransactionId(), amount);
        return SettlementResult.completed();
    }

    private SettlementResult failAtomically(String transactionId, SettlementAccount sender, String reason) {
        transactionStore.updateStatus(transactionId, TransactionStatus.FAILED);
        log.warn("Settlement failed transactionId={} reason={}", transactionId, reason);
        return SettlementResult.failed(reason);
    }

    private SettlementResult failAtomically(String transactionId, String reason) {
        transactionStore.updateStatus(transactionId, TransactionStatus.FAILED);
        log.warn("Settlement failed transactionId={} reason={}", transactionId, reason);
        return SettlementResult.failed(reason);
    }
}
