package com.example.delivery.modules.user.dto;

import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.enums.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员局部更新用户资料、角色和账号状态的请求 DTO。
 * 校验仅覆盖实际传入字段；仅管理员可用的接口边界阻止普通用户提交提权修改。
 */
public record AdminUserUpdateRequest(
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone must be a valid mobile number")
        String phone,
        @Size(max = 32) String nickname,
        UserRole role,
        UserStatus status) {
}
