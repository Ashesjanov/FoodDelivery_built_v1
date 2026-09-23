package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 当前用户局部资料更新请求；为 {@code null} 的字段由服务层保留原值，
 * 非空字段会在落库前去除首尾空白。
 */
public record UserProfileUpdateRequest(
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone must be a valid mobile number")
        String phone,
        @Size(max = 32) String nickname) {
}
