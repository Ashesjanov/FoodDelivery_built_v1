package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 优惠活动的运营状态，控制活动是否可预热、领取或核销。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 coupon.status。
 */
public enum CouponStatus {
    UPCOMING("UPCOMING"), ACTIVE("ACTIVE"), PAUSED("PAUSED"), EXPIRED("EXPIRED"), EXHAUSTED("EXHAUSTED");

    @EnumValue
    private final String code;

    CouponStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
