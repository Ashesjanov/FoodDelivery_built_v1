package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 骑手离线、在线、配送中和停用状态，用于派单资格判断。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 delivery_rider.status。
 */
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
