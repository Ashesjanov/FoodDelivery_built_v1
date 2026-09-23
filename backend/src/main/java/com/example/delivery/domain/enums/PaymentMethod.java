package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum PaymentMethod {
    MOCK_BALANCE("MOCK_BALANCE"), ALIPAY("ALIPAY"), WECHAT("WECHAT");

    @EnumValue
    private final String code;

    PaymentMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
