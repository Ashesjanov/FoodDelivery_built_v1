package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 购物车条目生命周期状态，服务购物车查询、结算和清理流程。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 写入 cart_item.status。
 */
public enum CartItemStatus {
    ACTIVE("ACTIVE"), CHECKED_OUT("CHECKED_OUT"), REMOVED("REMOVED");

    @EnumValue
    private final String code;

    CartItemStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
