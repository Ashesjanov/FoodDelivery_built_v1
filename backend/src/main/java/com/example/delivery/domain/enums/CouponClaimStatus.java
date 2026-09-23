package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

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
