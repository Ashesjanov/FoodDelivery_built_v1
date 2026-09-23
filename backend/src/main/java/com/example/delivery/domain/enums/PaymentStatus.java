package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum PaymentStatus {
    PENDING("PENDING"), SUCCESS("SUCCESS"), FAILED("FAILED"), CLOSED("CLOSED"), REFUNDED("REFUNDED");

    @EnumValue
    private final String code;

    PaymentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
