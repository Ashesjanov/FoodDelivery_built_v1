package com.example.delivery.modules.payment.dto;

import com.example.delivery.domain.enums.PaymentMethod;
import com.example.delivery.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
