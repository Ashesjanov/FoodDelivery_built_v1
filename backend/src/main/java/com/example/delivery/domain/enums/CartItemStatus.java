package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum CartItemStatus {
    ACTIVE("ACTIVE"), CHECKED_OUT("CHECKED_OUT"), REMOVED("REMOVED");

    @EnumValue
    private final String code;

    CartItemStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
