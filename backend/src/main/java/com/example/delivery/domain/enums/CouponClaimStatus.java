package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户已领取优惠券的核销状态，用于下单占券、支付成功用券和退单恢复。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 coupon_claim.status。
 */
public enum CouponClaimStatus {
    UNUSED("UNUSED"), USED("USED"), EXPIRED("EXPIRED"), CANCELLED("CANCELLED");

    @EnumValue
    private final String code;

    CouponClaimStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
