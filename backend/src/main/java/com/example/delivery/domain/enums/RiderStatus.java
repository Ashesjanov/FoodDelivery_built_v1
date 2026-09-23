package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum RiderStatus {
    OFFLINE("OFFLINE"), ONLINE("ONLINE"), BUSY("BUSY"), SUSPENDED("SUSPENDED");

    @EnumValue
    private final String code;

    RiderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
