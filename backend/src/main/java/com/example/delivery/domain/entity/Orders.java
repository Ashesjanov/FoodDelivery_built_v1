package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long addressId;
    private Long couponClaimId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packagingFee;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String userNote;
    private String merchantNote;
    private String cancelReason;
    private String contactPhone;
    private String deliveryAddressSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;
}
