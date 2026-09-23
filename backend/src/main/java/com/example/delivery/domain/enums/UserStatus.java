package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 账号可用状态；仅 ACTIVE 能通过 JWT 认证过滤器，锁定和停用均拒绝登录态。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 user_account.status。
 */
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
