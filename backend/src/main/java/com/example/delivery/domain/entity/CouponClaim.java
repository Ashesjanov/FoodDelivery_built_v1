package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.CouponClaimStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_claim")
public class CouponClaim {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long couponId;
    private Long userId;
    private Long orderId;
    private CouponClaimStatus status;
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
