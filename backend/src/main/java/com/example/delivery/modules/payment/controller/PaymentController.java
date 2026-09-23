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

/**
 * 支付 REST 控制器：提供 /api/payments 下的模拟支付和支付记录查询入口。
 * 仅依赖 {@link PaymentService}；所有端点要求登录，账户归属检查由服务层完成，
 * 控制器本身不开启事务，模拟支付的原子性由服务层事务保证。
 */
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
