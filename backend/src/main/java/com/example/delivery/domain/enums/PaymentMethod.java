package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 支付渠道类型，其中 MOCK_BALANCE 用于本地演示，其余为外部支付方式标识。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 payment_record.payment_method。
 */
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
