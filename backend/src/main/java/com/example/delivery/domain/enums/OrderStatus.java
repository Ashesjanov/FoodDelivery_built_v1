package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 订单从待支付、履约到完成、取消或退款的业务状态。
 * 状态推进由订单、支付和配送服务共同校验；code 经 @EnumValue 映射 orders.status。
 */
public enum OrderStatus {
    PENDING_PAYMENT("PENDING_PAYMENT"), PAID("PAID"), ACCEPTED("ACCEPTED"), PREPARING("PREPARING"),
    READY("READY"), PICKED_UP("PICKED_UP"), DELIVERED("DELIVERED"), COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"), REFUNDED("REFUNDED");

    @EnumValue
    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
