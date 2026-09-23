package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户角色，同时决定 JWT role claim 和 Spring Security 的 ROLE_* 权限。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 user_account.role。
 */
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
