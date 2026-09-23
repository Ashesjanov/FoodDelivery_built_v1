package com.example.delivery.modules.payment.dto;

import com.example.delivery.domain.enums.PaymentMethod;
import com.example.delivery.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付模块的请求和响应模型：包含模拟支付参数、退款原因和支付记录视图。
 * 仅承担字段约束与传输职责，不含状态判断，也不涉及事务或权限边界。
 */
public final class PaymentDto {
    private PaymentDto() {
    }

    public record CreateRequest(@NotNull Long orderId, @NotNull PaymentMethod paymentMethod) {
    }

    public record MockPayRequest(@Size(max = 100) String transactionNo) {
    }

    public record MockRequest(@NotNull Long orderId, @NotNull PaymentMethod paymentMethod,
                              @Size(max = 100) String transactionNo) {
    }

    public record RefundRequest(@Size(max = 200) String reason) {
    }

    public record View(Long id, String paymentNo, Long orderId, Long userId, BigDecimal amount, PaymentMethod paymentMethod,
                       PaymentStatus status, String transactionNo, String failureReason, BigDecimal refundedAmount,
                       LocalDateTime paidAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
