package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum DeliveryStatus {
    WAITING_RIDER("WAITING_RIDER"), ASSIGNED("ASSIGNED"), PICKED_UP("PICKED_UP"), DELIVERED("DELIVERED"), CANCELLED("CANCELLED");

    @EnumValue
    private final String code;

    DeliveryStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
