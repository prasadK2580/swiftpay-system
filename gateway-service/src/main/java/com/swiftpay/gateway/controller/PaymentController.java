package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.application.mapper.PaymentCommandMapper;
import com.swiftpay.gateway.controller.dto.PaymentRequest;
import com.swiftpay.gateway.controller.dto.PaymentResponse;
import com.swiftpay.gateway.service.PaymentInitiationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {


    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentInitiationUseCase paymentInitiationUseCase;

    public PaymentController(PaymentInitiationUseCase paymentInitiationUseCase) {
        this.paymentInitiationUseCase = paymentInitiationUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return paymentInitiationUseCase.initiatePayment(
                idempotencyKey, PaymentCommandMapper.fromRequest(request));
    }
}
