package com.swiftpay.gateway.service.validation;

import com.swiftpay.gateway.model.PaymentCommand;
import com.swiftpay.gateway.cache.PaymentBalanceCache;
import com.swiftpay.gateway.client.LedgerBalanceClient;
import com.swiftpay.gateway.service.PaymentIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestValidatorTest {

    @Mock
    private LedgerBalanceClient ledgerBalanceReader;
    @Mock
    private PaymentBalanceCache balanceStore;
    @Mock
    private PaymentIdempotencyService paymentIdempotencyService;

    @InjectMocks
    private PaymentRequestValidator validator;

    @Test
    void validateIdempotencyKeyHeader_trimsValue() {
        assertEquals("key-1", validator.validateIdempotencyKeyHeader("  key-1  "));
    }

    @Test
    void validateNewPayment_rejectsSelfTransfer() {
        PaymentCommand command = new PaymentCommand(1001L, 1001L, 10.0, "INR");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateNewPayment("key", command));
        verify(paymentIdempotencyService).recordValidationFailure(eq("key"), eq(command), any());
    }

    @Test
    void validateNewPayment_acceptsValidPayment() {
        PaymentCommand command = new PaymentCommand(1001L, 2002L, 10.0, "INR");
        when(ledgerBalanceReader.getBalance(1001L, "INR")).thenReturn(Optional.of(10000.0));
        when(ledgerBalanceReader.getBalance(2002L, "INR")).thenReturn(Optional.of(5000.0));
        when(balanceStore.getBalance(1001L, "INR")).thenReturn(Optional.of(10000.0));

        assertDoesNotThrow(() -> validator.validateNewPayment("key", command));
        verify(balanceStore).setBalance(1001L, 10000.0);
    }
}
