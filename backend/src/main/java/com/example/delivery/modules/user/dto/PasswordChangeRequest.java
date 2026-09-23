package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求，字段格式在 REST 边界校验；
 * 服务层还会验证当前密码，并要求新密码不得与原密码相同。
 */
public record PasswordChangeRequest(
        @NotBlank @Size(max = 64) String currentPassword,
        @NotBlank @Size(min = 8, max = 64) String newPassword) {
}
