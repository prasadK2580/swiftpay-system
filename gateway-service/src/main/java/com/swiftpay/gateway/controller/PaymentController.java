package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.application.mapper.PaymentCommandMapper;
import com.swiftpay.gateway.controller.dto.PaymentRequest;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.service.PaymentInitiationUseCase;
import com.swiftpay.gateway.service.PaymentQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments (Service A)", description = "Transaction Gateway — initiate and poll payment status")
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentInitiationUseCase paymentInitiationUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;

    public PaymentController(
            PaymentInitiationUseCase paymentInitiationUseCase,
            PaymentQueryUseCase paymentQueryUseCase) {
        this.paymentInitiationUseCase = paymentInitiationUseCase;
        this.paymentQueryUseCase = paymentQueryUseCase;
    }

    @Operation(
            summary = "Create payment",
            description = "Validates idempotency (Redis 24h), checks balance, persists PENDING, publishes PaymentInitiated.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment accepted (PENDING)"),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict"),
            @ApiResponse(responseCode = "422", description = "Insufficient funds or validation error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return paymentInitiationUseCase.initiatePayment(
                idempotencyKey, PaymentCommandMapper.fromRequest(request));
    }

    @Operation(
            summary = "Get payment status",
            description = "Poll transaction status after create (PENDING → COMPLETED/FAILED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current transaction state"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    public PaymentResponse getByTransactionId(
            @Parameter(description = "Server-assigned transaction id") @PathVariable String transactionId) {
        return paymentQueryUseCase.getByTransactionId(transactionId);
    }
}
