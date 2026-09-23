package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 支付流水状态，驱动订单支付确认、关单和退款幂等处理。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 payment_record.status。
 */
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
