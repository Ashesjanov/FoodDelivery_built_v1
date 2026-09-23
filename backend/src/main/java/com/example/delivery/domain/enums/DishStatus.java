package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum DishStatus {
    ON_SALE("ON_SALE"), OFF_SALE("OFF_SALE"), SOLD_OUT("SOLD_OUT"), DELETED("DELETED");

    @EnumValue
    private final String code;

    DishStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
