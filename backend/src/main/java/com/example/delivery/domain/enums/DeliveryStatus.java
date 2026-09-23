package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 配送单从等骑手、取餐到送达或取消的执行状态。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 delivery_record.status。
 */
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
