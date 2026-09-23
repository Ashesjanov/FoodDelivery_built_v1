package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

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
