package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

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
