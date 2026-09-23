package com.example.delivery.modules.order.dto;

import com.example.delivery.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单模块的请求和响应模型：覆盖购物车下单、取消原因、订单概要、明细和组合详情。
 * 本文件只负责数据边界及输入校验，不包含业务规则，也不参与事务或权限判断。
 */
public final class OrderDto {
    private OrderDto() {
    }

    public record CreateRequest(
            List<Long> cartItemIds,
            @NotNull Long addressId,
            Long couponClaimId,
            @Size(max = 30) String contactPhone,
            @Size(max = 200) String userNote
    ) {
    }

    public record ReasonRequest(@Size(max = 200) String reason) {
    }

    public record ItemView(Long id, Long dishId, Long dishSpecId, String dishName, String specName, String note,
                           BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
    }

    public record View(
            Long id, String orderNo, Long userId, Long merchantId, String merchantName, Long addressId,
            Long couponClaimId, OrderStatus status, BigDecimal totalAmount, BigDecimal deliveryFee,
            BigDecimal packagingFee, BigDecimal discountAmount, BigDecimal payableAmount, String userNote,
            String merchantNote, String cancelReason, String contactPhone, String deliveryAddressSnapshot,
            LocalDateTime createdAt, LocalDateTime acceptedAt, LocalDateTime readyAt, LocalDateTime pickedAt,
            LocalDateTime deliveredAt, LocalDateTime cancelledAt, LocalDateTime updatedAt
    ) {
    }

    public record Detail(View order, List<ItemView> items) {
    }
}
