package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户名和密码登录请求；REST 边界的 Bean Validation 会先拒绝空值或超长凭据，
 * 再进入认证流程。
 */
public record LoginRequest(
        @NotBlank @Size(max = 32) String username,
        @NotBlank @Size(max = 64) String password) {
}
