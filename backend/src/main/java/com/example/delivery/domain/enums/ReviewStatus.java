package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ReviewStatus {
    VISIBLE("VISIBLE"), HIDDEN("HIDDEN"), DELETED("DELETED");

    @EnumValue
    private final String code;

    ReviewStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
