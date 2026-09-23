package com.example.delivery.modules.coupon.dto;

import com.example.delivery.domain.enums.CouponClaimStatus;
import com.example.delivery.domain.enums.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CouponDto {
    private CouponDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 40) String code,
            @NotNull CouponType couponType,
            @NotNull @DecimalMin("0.00") BigDecimal thresholdAmount,
            @DecimalMin("0.00") BigDecimal discountAmount,
            @DecimalMin("0.0001") BigDecimal discountRate,
            @NotNull @Min(1) Integer totalQuantity,
            @Min(1) Integer perUserLimit,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {
    }

    public record CouponView(Long id, String name, String code, CouponType couponType,
                             com.example.delivery.domain.enums.CouponStatus status, BigDecimal thresholdAmount,
                             BigDecimal discountAmount, BigDecimal discountRate, Integer totalQuantity,
                             Integer claimedQuantity, Integer remainingQuantity, Integer perUserLimit,
                             LocalDateTime startTime, LocalDateTime endTime) {
    }

    public record ClaimView(Long id, Long couponId, Long orderId, CouponClaimStatus status, String couponName,
                            CouponType couponType, BigDecimal thresholdAmount, BigDecimal discountAmount,
                            BigDecimal discountRate, LocalDateTime claimedAt, LocalDateTime usedAt, LocalDateTime expiresAt) {
    }
}
