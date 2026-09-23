package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 菜品上架、下架、售罄和软删除状态，用于菜单展示与可售校验。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 dish.status。
 */
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
