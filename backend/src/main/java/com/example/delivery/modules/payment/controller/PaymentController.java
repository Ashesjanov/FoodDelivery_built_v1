package com.example.delivery.modules.payment.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.modules.payment.dto.PaymentDto;
import com.example.delivery.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("isAuthenticated()")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mock")
    public ApiResponse<PaymentDto.View> mock(@Valid @RequestBody PaymentDto.MockRequest request) {
        return ApiResponse.created(paymentService.mock(request));
    }

    @PostMapping("/orders/{orderId}/mock-pay")
    public ApiResponse<PaymentDto.View> mockPay(@PathVariable Long orderId,
                                                @Valid @RequestBody(required = false) PaymentDto.MockPayRequest request) {
        return ApiResponse.created(paymentService.mockPay(orderId,
                request == null ? new PaymentDto.MockPayRequest(null) : request));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDto.View> get(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.get(paymentId));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<PaymentDto.View> latestForOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.latestForOrder(orderId));
    }
}
