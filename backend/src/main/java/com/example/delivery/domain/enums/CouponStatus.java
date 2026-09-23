package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

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
