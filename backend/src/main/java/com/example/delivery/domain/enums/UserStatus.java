package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserStatus {
    ACTIVE("ACTIVE"), LOCKED("LOCKED"), DISABLED("DISABLED");

    @EnumValue
    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
