package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum MerchantBusinessStatus {
    PREPARING("PREPARING"), OPEN("OPEN"), PAUSED("PAUSED"), CLOSED("CLOSED");

    @EnumValue
    private final String code;

    MerchantBusinessStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
