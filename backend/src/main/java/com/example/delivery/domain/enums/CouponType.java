package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 优惠计算方式：FIXED 使用固定减免，PERCENT 使用折扣比例。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 coupon.coupon_type。
 */
public enum CouponType {
    FIXED("FIXED"), PERCENT("PERCENT");

    @EnumValue
    private final String code;

    CouponType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
