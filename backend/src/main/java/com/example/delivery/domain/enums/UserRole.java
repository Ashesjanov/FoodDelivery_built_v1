package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserRole {
    CUSTOMER("CUSTOMER"), ADMIN("ADMIN"), MERCHANT("MERCHANT"), RIDER("RIDER");

    @EnumValue
    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
